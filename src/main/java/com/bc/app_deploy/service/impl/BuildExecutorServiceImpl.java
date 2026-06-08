package com.bc.app_deploy.service.impl;

import com.bc.app_deploy.model.entity.BuildDO;
import com.bc.app_deploy.model.entity.ProjectDO;
import com.bc.app_deploy.model.entity.ServerDO;
import com.bc.app_deploy.service.*;
import com.bc.app_deploy.utils.BuildUtils;
import com.bc.app_deploy.utils.GitUtils;
import com.bc.app_deploy.utils.SshUtils;
import com.bc.app_deploy.utils.websocket.BuildLogSocket;
import com.jcraft.jsch.Session;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
@Slf4j
@Service
public class BuildExecutorServiceImpl implements IBuildExecutorService {
    @Resource
    private IBuildService buildService;

    @Resource
    private IProjectService projectService;

    @Resource
    private IServerService serverService;

    @Resource
    private IProjectServerService projectServerService;

    @Value("${deploy.workspace:}")
    private String workspace;

    @Value("${deploy.keep-workspace:false}")
    private Boolean keepWorkspace;

    /**
     * 异步执行构建任务
     */
    @Async("buildExecutor")
    public void executeBuild(Long buildId) {
        BuildDO build = buildService.getById(buildId);
        ProjectDO project = projectService.getById(build.getProjectId());

        // 更新状态为运行中
        build.setStatus("RUNNING");
        build.setStartTime(LocalDateTime.now());
        buildService.updateById(build);

        StringBuilder logBuilder = new StringBuilder();

        try {
            // 日志输出回调
            java.util.function.Consumer<String> logConsumer = msg -> {
                logBuilder.append(msg).append("\n");
                BuildLogSocket.sendMessage(buildId.toString(), msg);
                log.info("[Build-{}] {}", buildId, msg);
            };

            logConsumer.accept("========================================");
            logConsumer.accept("开始构建项目: " + project.getName());
            logConsumer.accept("========================================");

            // 1. 准备工作目录（使用系统临时目录，避免文件锁定问题）
            File projectDir = createWorkspaceDir(project.getId(), logConsumer);

            try {

                // 2. 拉取代码
                logConsumer.accept("\n[步骤1] 拉取Git代码");
                GitUtils.cloneOrPull(project.getGitUrl(), project.getBranch(), project.getGitAccount(), project.getGitPassword(), projectDir, logConsumer);

                // 2.5. 处理项目子目录（如果配置了 projectDir）
                File actualProjectDir = projectDir;
                if (project.getProjectDir() != null && !project.getProjectDir().trim().isEmpty()) {
                    actualProjectDir = new File(projectDir, project.getProjectDir());
                    if (!actualProjectDir.exists()) {
                        throw new Exception("项目目录不存在: " + project.getProjectDir());
                    }
                    logConsumer.accept("[构建] 检测到项目子目录，切换到: " + actualProjectDir.getAbsolutePath());
                }

                // 3. 清理旧的构建产物（解决文件被占用问题）
                logConsumer.accept("\n[步骤2] 清理旧的构建产物");
                BuildUtils.cleanBuildDirectory(actualProjectDir, project.getBuildDir(), logConsumer);

                // 4. 执行构建
                logConsumer.accept("\n[步骤3] 执行构建命令");

                // Vue 项目需要先执行 npm install
                String buildCommand = project.getBuildCommand();
                if ("VUE".equalsIgnoreCase(project.getProjectType())) {
                    // 检查是否已包含npm install
                    if(!buildCommand.contains("npm install") && !buildCommand.contains("npm ci")) {
                        logConsumer.accept("[Build]检测到 vue 项目，自动执行 npm install");
                        boolean installSuccess = BuildUtils.executeCommand("npm install", actualProjectDir, logConsumer);
                        if (!installSuccess) {
                            throw new Exception("npm install 失败");
                        }
                    }
                }
                boolean buildSuccess = BuildUtils.executeCommand(project.getBuildCommand(), actualProjectDir, logConsumer);

                if (!buildSuccess) {
                    throw new Exception("构建失败");
                }

                // 5. 查找构建产物
                logConsumer.accept("\n[步骤4] 查找构建产物");
                File artifact = BuildUtils.findBuildArtifact(actualProjectDir, project.getBuildDir());
                logConsumer.accept("[Build] 找到构建产物: " + artifact.getAbsolutePath());

                // 6. 部署到服务器（支持多服务器）
                if (project.getAutoDeploy() != null && project.getAutoDeploy() == 1) {
                    logConsumer.accept("\n[步骤5] 部署到服务器");

                    // 获取项目关联的所有服务器
                    List<Long> serverIds = projectServerService.getProjectServerIds(project.getId());

                    if (serverIds == null || serverIds.isEmpty()) {
                        logConsumer.accept("[警告] 未配置服务器，跳过部署步骤");
                    } else {
                        logConsumer.accept("[Deploy] 共需部署到 " + serverIds.size() + " 台服务器");

                        // 使用原子计数器统计成功和失败数量
                        AtomicInteger successCount = new AtomicInteger(0);
                        AtomicInteger failCount = new AtomicInteger(0);

                        List<ServerDO> servers = serverService.listByIds(serverIds);
                        Map<Long, ServerDO> serverGroup = servers.stream()
                                .collect(Collectors.toMap(ServerDO::getId, Function.identity()));

                        // 使用CompletableFuture并行部署到所有服务器
                        List<CompletableFuture<Void>> deployFutures = serverIds.stream()
                                .map(serverId -> CompletableFuture.runAsync(() -> {
                                    ServerDO server = serverGroup.get(serverId);
                                    int index = serverIds.indexOf(serverId);

                                    if (server == null) {
                                        logConsumer.accept("\n[服务器" + (index + 1) + "/" + serverIds.size() + "] 服务器不存在(ID:" + serverId + ")，跳过");
                                        failCount.incrementAndGet();
                                        return;
                                    }

                                    try {
                                        logConsumer.accept("\n[服务器" + (index + 1) + "/" + serverIds.size() + "] 开始部署到: " + server.getName() + " (" + server.getHost() + ")");
                                        deployToServer(server, project, artifact, logConsumer);
                                        logConsumer.accept("[服务器" + (index + 1) + "/" + serverIds.size() + "] 部署成功");
                                        successCount.incrementAndGet();
                                    } catch (Exception e) {
                                        logConsumer.accept("[服务器" + (index + 1) + "/" + serverIds.size() + "] 部署失败: " + e.getMessage());
                                        failCount.incrementAndGet();
                                        log.error("部署到服务器失败: " + server.getHost(), e);
                                    }
                                }))
                                .toList();

                        // 等待所有部署任务完成
                        CompletableFuture.allOf(deployFutures.toArray(new CompletableFuture[0])).join();

                        logConsumer.accept("\n[Deploy] 部署完成: 成功 " + successCount.get() + " 台，失败 " + failCount.get() + " 台");

                        // 如果所有服务器都部署失败，则抛出异常
                        if (failCount.get() > 0 && successCount.get() == 0) {
                            throw new Exception("所有服务器部署均失败");
                        }
                    }
                } else {
                    logConsumer.accept("\n[跳过] 未开启自动部署，跳过部署步骤");
                }

                // 构建成功
                logConsumer.accept("\n========================================");
                logConsumer.accept("构建成功！");
                logConsumer.accept("========================================");

                build.setStatus("SUCCESS");

            } finally {
                // 清理工作目录（除非配置了保留）
                if (!keepWorkspace) {
                    cleanupWorkspaceDir(projectDir, logConsumer);
                }
            }

        } catch (Exception e) {
            log.error("构建失败", e);
            logBuilder.append("\n[错误] ").append(e.getMessage()).append("\n");
            BuildLogSocket.sendMessage(buildId.toString(), "[错误] " + e.getMessage());

            build.setStatus("FAILED");
        } finally {
            // 更新构建记录
            build.setEndTime(LocalDateTime.now());
            build.setDuration(ChronoUnit.SECONDS.between(build.getStartTime(), build.getEndTime()));
            build.setLog(logBuilder.toString());
            buildService.updateById(build);
        }
    }

    /**
     * 创建工作目录
     */
    private File createWorkspaceDir(Long projectId, java.util.function.Consumer<String> logConsumer) {
        String baseDir;

        if (workspace != null && !workspace.isEmpty()) {
            // 使用配置的工作目录
            baseDir = workspace;
        } else {
            // 根据操作系统使用默认目录
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                // Windows: D:\app-deploy
                baseDir = "D:\\app-deploy";
            } else {
                // Linux/Mac: /home/app-deploy
                baseDir = "/home/app-deploy";
            }
        }

        String dirName = String.format("project-%d-%d", projectId, System.currentTimeMillis());
        File workDir = new File(baseDir, dirName);

        if (!workDir.exists()) {
            workDir.mkdirs();
        }

        logConsumer.accept("[Workspace] 工作目录: " + workDir.getAbsolutePath());
        return workDir;
    }

    /**
     * 清理工作目录
     */
    private void cleanupWorkspaceDir(File projectDir, java.util.function.Consumer<String> logConsumer) {
        if (projectDir == null || !projectDir.exists()) {
            return;
        }

        logConsumer.accept("\n[清理] 开始清理工作目录...");

        try {
            // Windows下可能文件被占用，重试3次
            int maxRetries = 3;
            boolean deleted = false;

            for (int i = 0; i < maxRetries && !deleted; i++) {
                try {
                    Files.walk(projectDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                    deleted = true;
                    logConsumer.accept("[清理] 工作目录清理成功");
                } catch (Exception e) {
                    if (i < maxRetries - 1) {
                        logConsumer.accept("[清理] 清理失败，等待重试... (" + (i + 1) + "/" + maxRetries + ")");
                        Thread.sleep(1000);
                    } else {
                        logConsumer.accept("[清理] 工作目录清理失败（可能被占用），将在下次构建时自动清理: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logConsumer.accept("[清理] 清理过程出错: " + e.getMessage());
        }
    }

    /**
     * 部署到服务器
     */
    private void deployToServer(ServerDO server, ProjectDO project, File artifact, java.util.function.Consumer<String> logConsumer) throws Exception {

        logConsumer.accept("[Deploy] 连接服务器: " + server.getHost());

        Session session = null;
        try {
            // 创建SSH连接（需要传入 privateKey 参数）
            session = SshUtils.createSession(server.getHost(), server.getPort(), server.getAccount(), server.getPassword(), server.getPrivateKey());
            session.connect();
            logConsumer.accept("[Deploy] SSH连接成功");

            // 上传文件
            String uploadPath = project.getDeployPath();
            if (uploadPath == null || uploadPath.trim().isEmpty()) {
                uploadPath = "/home/deploy/";
            }
            // 确保路径以 / 结尾
            if (!uploadPath.endsWith("/")) {
                uploadPath += "/";
            }
            uploadPath += project.getName();

            logConsumer.accept("[Deploy] 创建部署目录: " + uploadPath);
            SshUtils.executeCommand(session, "mkdir -p " + uploadPath, logConsumer);

            // 在上传新文件之前，先备份旧的jar文件（如果存在）
            logConsumer.accept("[Deploy] 备份旧版本jar文件...");
            String backupCommand = String.format(
                    "cd %s && " +
                            "if ls *.jar 1> /dev/null 2>&1; then " +
                            "  for jar in *.jar; do " +
                            "    if [ -f \"$jar\" ]; then " +
                            "      timestamp=$(date '+%%Y-%%m-%%d_%%H:%%M:%%S'); " +
                            "      backup_name=\"${jar%%.jar}_${timestamp}.jar.bak\"; " +
                            "      mv \"$jar\" \"$backup_name\"; " +
                            "      echo \"已备份: $jar -> $backup_name\"; " +
                            "    fi; " +
                            "  done; " +
                            "  ls -t *.jar.bak 2>/dev/null | tail -n +4 | xargs rm -f 2>/dev/null; " +
                            "  echo \"清理旧备份，保留最近3个\"; " +
                            "else " +
                            "  echo \"未找到旧jar文件，无需备份\"; " +
                            "fi",
                    uploadPath
            );
            SshUtils.executeCommand(session, backupCommand, logConsumer);

            logConsumer.accept("[Deploy] 上传构建产物...");
            SshUtils.uploadFile(session, artifact, uploadPath, logConsumer);
            logConsumer.accept("[Deploy] 文件上传成功");

            // 执行部署脚本
            if (project.getDeployScript() != null && !project.getDeployScript().isEmpty()) {
                logConsumer.accept("[Deploy] 执行部署脚本...");
                logConsumer.accept("[Deploy] 应用端口: " + project.getAppPort());

                // 替换脚本中的变量
                String script = project.getDeployScript()
                        .replace("{{uploadPath}}", uploadPath)
                        .replace("{{appPort}}", String.valueOf(project.getAppPort()));

                // 将脚本写入临时文件并执行
                String scriptPath = uploadPath + "/deploy.sh";
                SshUtils.executeCommand(session, "cat > " + scriptPath + " << 'EOF'\n" + script + "\nEOF", logConsumer);
                SshUtils.executeCommand(session, "chmod +x " + scriptPath, logConsumer);
                SshUtils.executeCommand(session, "bash " + scriptPath, logConsumer);

                logConsumer.accept("[Deploy] 部署脚本执行完成");
            }

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 异步执行版本回退（根据备份文件名）
     */
    @Async("buildExecutor")
    public void executeRollbackByBackup(Long buildId, Long projectId, String backupFileName) {
        BuildDO build = buildService.getById(buildId);
        ProjectDO project = projectService.getById(projectId);

        // 更新状态为运行中
        build.setStatus("RUNNING");
        build.setStartTime(LocalDateTime.now());
        buildService.updateById(build);

        StringBuilder logBuilder = new StringBuilder();

        try {
            // 日志输出回调
            java.util.function.Consumer<String> logConsumer = msg -> {
                logBuilder.append(msg).append("\n");
                BuildLogSocket.sendMessage(buildId.toString(), msg);
                log.info("[Rollback-{}] {}", buildId, msg);
            };

            logConsumer.accept("========================================");
            logConsumer.accept("开始版本回退: " + project.getName());
            logConsumer.accept("备份文件: " + backupFileName);
            logConsumer.accept("========================================");

            // 部署到服务器（支持多服务器）
            if (project.getAutoDeploy() != null && project.getAutoDeploy() == 1) {
                logConsumer.accept("\n[步骤1] 开始回退部署");

                // 获取项目关联的所有服务器
                List<Long> serverIds = projectServerService.getProjectServerIds(project.getId());

                if (serverIds == null || serverIds.isEmpty()) {
                    throw new Exception("未配置服务器，无法执行回退");
                }

                logConsumer.accept("[回退] 共需回退 " + serverIds.size() + " 台服务器");

                // 使用原子计数器统计成功和失败数量
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failCount = new AtomicInteger(0);

                List<ServerDO> servers = serverService.listByIds(serverIds);
                Map<Long, ServerDO> serverGroup = servers.stream()
                        .collect(Collectors.toMap(ServerDO::getId, Function.identity()));

                // 使用CompletableFuture并行回退到所有服务器
                List<CompletableFuture<Void>> rollbackFutures = serverIds.stream()
                        .map(serverId -> CompletableFuture.runAsync(() -> {
                            ServerDO server = serverGroup.get(serverId);
                            int index = serverIds.indexOf(serverId);

                            if (server == null) {
                                logConsumer.accept("\n[服务器" + (index + 1) + "/" + serverIds.size() + "] 服务器不存在(ID:" + serverId + ")，跳过");
                                failCount.incrementAndGet();
                                return;
                            }

                            try {
                                logConsumer.accept("\n[服务器" + (index + 1) + "/" + serverIds.size() + "] 开始回退: " + server.getName() + " (" + server.getHost() + ")");
                                rollbackByBackupOnServer(server, project, backupFileName, logConsumer);
                                logConsumer.accept("[服务器" + (index + 1) + "/" + serverIds.size() + "] 回退成功");
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                logConsumer.accept("[服务器" + (index + 1) + "/" + serverIds.size() + "] 回退失败: " + e.getMessage());
                                failCount.incrementAndGet();
                                log.error("回退到服务器失败: " + server.getHost(), e);
                            }
                        }))
                        .collect(Collectors.toList());

                // 等待所有回退任务完成
                CompletableFuture.allOf(rollbackFutures.toArray(new CompletableFuture[0])).join();

                logConsumer.accept("\n[回退] 回退完成: 成功 " + successCount.get() + " 台，失败 " + failCount.get() + " 台");

                if (failCount.get() > 0 && successCount.get() == 0) {
                    throw new Exception("所有服务器回退均失败");
                }
            } else {
                throw new Exception("项目未开启自动部署，无法执行回退");
            }

            // 回退成功
            logConsumer.accept("\n========================================");
            logConsumer.accept("回退成功！");
            logConsumer.accept("========================================");

            build.setStatus("SUCCESS");

        } catch (Exception e) {
            log.error("回退失败", e);
            logBuilder.append("\n[错误] ").append(e.getMessage()).append("\n");
            BuildLogSocket.sendMessage(buildId.toString(), "[错误] " + e.getMessage());

            build.setStatus("FAILED");
        } finally {
            // 更新构建记录
            build.setEndTime(LocalDateTime.now());
            build.setDuration(ChronoUnit.SECONDS.between(build.getStartTime(), build.getEndTime()));
            build.setLog(logBuilder.toString());
            buildService.updateById(build);
        }
    }

    /**
     * 在服务器上执行回退（根据备份文件名）
     */
    private void rollbackByBackupOnServer(ServerDO server, ProjectDO project, String backupFileName, java.util.function.Consumer<String> logConsumer) throws Exception {
        logConsumer.accept("[回退] 连接服务器: " + server.getHost());

        Session session = null;
        try {
            session = SshUtils.createSession(server.getHost(), server.getPort(), server.getAccount(), server.getPassword(), server.getPrivateKey());
            session.connect();
            logConsumer.accept("[回退] SSH连接成功");

            // 构建部署路径
            String uploadPath = project.getDeployPath();
            if (uploadPath == null || uploadPath.trim().isEmpty()) {
                uploadPath = "/home/deploy/";
            }
            if (!uploadPath.endsWith("/")) {
                uploadPath += "/";
            }
            uploadPath += project.getName();

            logConsumer.accept("[回退] 使用备份文件: " + backupFileName);

            // 执行回退脚本
            logConsumer.accept("[回退] 开始执行回退脚本...");

            String rollbackScript = generateRollbackScript(uploadPath, backupFileName, project.getAppPort());

            // 将脚本写入临时文件并执行
            String scriptPath = uploadPath + "/rollback.sh";
            SshUtils.executeCommand(session, "cat > " + scriptPath + " << 'EOF'\n" + rollbackScript + "\nEOF", logConsumer);
            SshUtils.executeCommand(session, "chmod +x " + scriptPath, logConsumer);
            SshUtils.executeCommand(session, "bash " + scriptPath, logConsumer);

            logConsumer.accept("[回退] 回退脚本执行完成");

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
    @Async("buildExecutor")
    public void executeRollback(Long buildId, Long targetBuildId) {
        BuildDO build = buildService.getById(buildId);
        BuildDO targetBuild = buildService.getById(targetBuildId);
        ProjectDO project = projectService.getById(build.getProjectId());

        // 更新状态为运行中
        build.setStatus("RUNNING");
        build.setStartTime(LocalDateTime.now());
        buildService.updateById(build);

        StringBuilder logBuilder = new StringBuilder();

        try {
            // 日志输出回调
            java.util.function.Consumer<String> logConsumer = msg -> {
                logBuilder.append(msg).append("\n");
                BuildLogSocket.sendMessage(buildId.toString(), msg);
                log.info("[Rollback-{}] {}", buildId, msg);
            };

            logConsumer.accept("========================================");
            logConsumer.accept("开始版本回退: " + project.getName());
            logConsumer.accept("目标构建 ID: " + targetBuildId);
            logConsumer.accept("目标构建时间: " + targetBuild.getCreatedAt());
            logConsumer.accept("========================================");

            // 部署到服务器（支持多服务器）
            if (project.getAutoDeploy() != null && project.getAutoDeploy() == 1) {
                logConsumer.accept("\n[步骤1] 开始回退部署");

                // 获取项目关联的所有服务器
                List<Long> serverIds = projectServerService.getProjectServerIds(project.getId());

                if (serverIds == null || serverIds.isEmpty()) {
                    throw new Exception("未配置服务器，无法执行回退");
                }

                logConsumer.accept("[回退] 共需回退 " + serverIds.size() + " 台服务器");

                // 使用原子计数器统计成功和失败数量
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failCount = new AtomicInteger(0);

                List<ServerDO> servers = serverService.listByIds(serverIds);
                Map<Long, ServerDO> serverGroup = servers.stream()
                        .collect(Collectors.toMap(ServerDO::getId, Function.identity()));

                // 使用CompletableFuture并行回退到所有服务器
                List<CompletableFuture<Void>> rollbackFutures = serverIds.stream()
                        .map(serverId -> CompletableFuture.runAsync(() -> {
                            ServerDO server = serverGroup.get(serverId);
                            int index = serverIds.indexOf(serverId);

                            if (server == null) {
                                logConsumer.accept("\n[服务器" + (index + 1) + "/" + serverIds.size() + "] 服务器不存在(ID:" + serverId + ")，跳过");
                                failCount.incrementAndGet();
                                return;
                            }

                            try {
                                logConsumer.accept("\n[服务器" + (index + 1) + "/" + serverIds.size() + "] 开始回退: " + server.getName() + " (" + server.getHost() + ")");
                                rollbackOnServer(server, project, targetBuild, logConsumer);
                                logConsumer.accept("[服务器" + (index + 1) + "/" + serverIds.size() + "] 回退成功");
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                logConsumer.accept("[服务器" + (index + 1) + "/" + serverIds.size() + "] 回退失败: " + e.getMessage());
                                failCount.incrementAndGet();
                                log.error("回退到服务器失败: " + server.getHost(), e);
                            }
                        }))
                        .collect(Collectors.toList());

                // 等待所有回退任务完成
                CompletableFuture.allOf(rollbackFutures.toArray(new CompletableFuture[0])).join();

                logConsumer.accept("\n[回退] 回退完成: 成功 " + successCount.get() + " 台，失败 " + failCount.get() + " 台");

                if (failCount.get() > 0 && successCount.get() == 0) {
                    throw new Exception("所有服务器回退均失败");
                }
            } else {
                throw new Exception("项目未开启自动部署，无法执行回退");
            }

            // 回退成功
            logConsumer.accept("\n========================================");
            logConsumer.accept("回退成功！");
            logConsumer.accept("========================================");

            build.setStatus("SUCCESS");

        } catch (Exception e) {
            log.error("回退失败", e);
            logBuilder.append("\n[错误] ").append(e.getMessage()).append("\n");
            BuildLogSocket.sendMessage(buildId.toString(), "[错误] " + e.getMessage());

            build.setStatus("FAILED");
        } finally {
            // 更新构建记录
            build.setEndTime(LocalDateTime.now());
            build.setDuration(ChronoUnit.SECONDS.between(build.getStartTime(), build.getEndTime()));
            build.setLog(logBuilder.toString());
            buildService.updateById(build);
        }
    }

    /**
     * 在服务器上执行回退
     */
    private void rollbackOnServer(ServerDO server, ProjectDO project, BuildDO targetBuild, java.util.function.Consumer<String> logConsumer) throws Exception {
        logConsumer.accept("[回退] 连接服务器: " + server.getHost());

        Session session = null;
        try {
            session = SshUtils.createSession(server.getHost(), server.getPort(), server.getAccount(), server.getPassword(), server.getPrivateKey());
            session.connect();
            logConsumer.accept("[回退] SSH连接成功");

            // 构建部署路径
            String uploadPath = project.getDeployPath();
            if (uploadPath == null || uploadPath.trim().isEmpty()) {
                uploadPath = "/home/deploy/";
            }
            if (!uploadPath.endsWith("/")) {
                uploadPath += "/";
            }
            uploadPath += project.getName();

            // 根据目标构建的创建时间查找备份文件
            LocalDateTime createTime = targetBuild.getCreatedAt();
            String backupTimePattern = createTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));

            logConsumer.accept("[回退] 查找备份文件: " + backupTimePattern + "*");

            // 查找匹配的备份文件
            String findBackupCommand = String.format(
                    "cd %s && ls -t *.jar.bak 2>/dev/null | grep '%s' | head -n 1",
                    uploadPath, backupTimePattern
            );

            // 执行命令并获取结果
            StringBuilder backupFile = new StringBuilder();
            SshUtils.executeCommand(session, findBackupCommand, output -> {
                if (output != null && !output.contains("命令执行完成") && !output.trim().isEmpty()) {
                    backupFile.append(output.trim());
                }
            });

            if (backupFile.length() == 0) {
                throw new Exception("未找到匹配的备份文件，请检查备份是否存在");
            }

            String backupFileName = backupFile.toString();
            logConsumer.accept("[回退] 找到备份文件: " + backupFileName);

            // 执行回退脚本
            logConsumer.accept("[回退] 开始执行回退脚本...");

            String rollbackScript = generateRollbackScript(uploadPath, backupFileName, project.getAppPort());

            // 将脚本写入临时文件并执行
            String scriptPath = uploadPath + "/rollback.sh";
            SshUtils.executeCommand(session, "cat > " + scriptPath + " << 'EOF'\n" + rollbackScript + "\nEOF", logConsumer);
            SshUtils.executeCommand(session, "chmod +x " + scriptPath, logConsumer);
            SshUtils.executeCommand(session, "bash " + scriptPath, logConsumer);

            logConsumer.accept("[回退] 回退脚本执行完成");

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 生成回退脚本（简化版）
     */
    private String generateRollbackScript(String deployPath, String backupFileName, Integer appPort) {
        return String.format(
                "#!/bin/bash\n" +
                        "# 版本回退脚本\n" +
                        "\n" +
                        "APP_PORT=%d\n" +
                        "DEPLOY_DIR=\"%s\"\n" +
                        "BACKUP_FILE=\"%s\"\n" +
                        "LOG_FILE=\"$DEPLOY_DIR/app.log\"\n" +
                        "\n" +
                        "echo \"========================================\"\n" +
                        "echo \"开始版本回退\"\n" +
                        "echo \"========================================\"\n" +
                        "\n" +
                        "# 1. 停止当前进程\n" +
                        "echo \"[步骤1] 正在停止当前进程...\"\n" +
                        "PID=$(lsof -t -i:$APP_PORT 2>/dev/null)\n" +
                        "if [ ! -z \"$PID\" ]; then\n" +
                        "  kill -15 $PID\n" +
                        "  sleep 3\n" +
                        "  if ps -p $PID > /dev/null 2>&1; then\n" +
                        "    kill -9 $PID\n" +
                        "    echo \"已强制停止进程: $PID\"\n" +
                        "  else\n" +
                        "    echo \"已优雅停止进程: $PID\"\n" +
                        "  fi\n" +
                        "else\n" +
                        "  echo \"未找到运行中的进程\"\n" +
                        "fi\n" +
                        "\n" +
                        "sleep 2\n" +
                        "\n" +
                        "# 2. 清理当前jar文件\n" +
                        "echo \"[步骤2] 清理当前版本...\"\n" +
                        "cd $DEPLOY_DIR\n" +
                        "rm -f *.jar\n" +
                        "echo \"已清理旧jar文件\"\n" +
                        "\n" +
                        "# 3. 恢复备份文件\n" +
                        "echo \"[步骤3] 恢复备份文件...\"\n" +
                        "if [ ! -f \"$BACKUP_FILE\" ]; then\n" +
                        "  echo \"错误: 备份文件不存在\"\n" +
                        "  exit 1\n" +
                        "fi\n" +
                        "\n" +
                        "# 提取原始jar名称（去掉所有时间戳和before_rollback标记）\n" +
                        "BACKUP_BASE=$(basename \"$BACKUP_FILE\")\n" +
                        "# 去掉.bak后缀\n" +
                        "NAME_WITHOUT_BAK=\"${BACKUP_BASE%%.bak}\"\n" +
                        "# 使用sed去掉所有时间戳模式: _yyyy-MM-dd_HH_mm_ss 或 _yyyy-MM-dd_HH:mm:ss\n" +
                        "CLEAN_NAME=$(echo \"$NAME_WITHOUT_BAK\" | sed -E 's/_[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}[_:][0-9]{2}[_:][0-9]{2}//g')\n" +
                        "# 去掉所有 _before_rollback 标记\n" +
                        "CLEAN_NAME=$(echo \"$CLEAN_NAME\" | sed 's/_before_rollback//g')\n" +
                        "\n" +
                        "JAR_NAME=\"$DEPLOY_DIR/$CLEAN_NAME\"\n" +
                        "cp \"$BACKUP_FILE\" \"$JAR_NAME\"\n" +
                        "echo \"已恢复: $(basename \"$JAR_NAME\")\"\n" +
                        "\n" +
                        "# 4. 启动应用\n" +
                        "echo \"[步骤4] 正在启动应用...\"\n" +
                        "> $LOG_FILE\n" +
                        "nohup java -jar -Xms512m -Xmx1024m -Dserver.port=$APP_PORT \"$JAR_NAME\" > $LOG_FILE 2>&1 &\n" +
                        "NEW_PID=$!\n" +
                        "echo \"应用已启动，PID: $NEW_PID\"\n" +
                        "echo \"JAR文件: $(basename \"$JAR_NAME\")\"\n" +
                        "echo \"日志文件: $LOG_FILE\"\n" +
                        "\n" +
                        "# 5. 等待应用启动\n" +
                        "echo \"[步骤5] 等待应用启动...\"\n" +
                        "TIMEOUT=30\n" +
                        "COUNT=0\n" +
                        "\n" +
                        "while [ $COUNT -lt $TIMEOUT ]; do\n" +
                        "  sleep 1\n" +
                        "  COUNT=$((COUNT + 1))\n" +
                        "  \n" +
                        "  if lsof -t -i:$APP_PORT > /dev/null 2>&1; then\n" +
                        "    echo \"========================================\"\n" +
                        "    echo \"回退成功！\"\n" +
                        "    echo \"应用端口: $APP_PORT\"\n" +
                        "    echo \"应用PID: $NEW_PID\"\n" +
                        "    echo \"========================================\"\n" +
                        "    exit 0\n" +
                        "  fi\n" +
                        "done\n" +
                        "\n" +
                        "echo \"警告: 应用在${TIMEOUT}秒内未成功启动，请检查日志: $LOG_FILE\"\n" +
                        "exit 1\n",
                appPort, deployPath, backupFileName
        );
    }
}
