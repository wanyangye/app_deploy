package com.bc.app_deploy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.BuildMapper;
import com.bc.app_deploy.model.entity.BuildDO;
import com.bc.app_deploy.model.entity.ProjectDO;
import com.bc.app_deploy.model.entity.ServerDO;
import com.bc.app_deploy.service.*;
import com.bc.app_deploy.utils.SshUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class BuildServiceImpl extends ServiceImpl<BuildMapper, BuildDO> implements IBuildService {
    @Resource
    private IProjectService projectService;

    @Resource
    @Lazy
    private IBuildExecutorService buildExecutorService;

    @Resource
    private IProjectServerService projectServerService;

    @Resource
    private IServerService serverService;

    /**
     * 触发构建
     */
    public Long triggerBuild(Long projectId) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        // 创建构建记录
        BuildDO build = new BuildDO();
        build.setProjectId(projectId);
        build.setStatus("PENDING");
        build.setTriggerBy(StpUtil.getLoginIdAsString());
        this.save(build);

        // 异步执行构建（通过外部Service调用，确保异步生效）
        buildExecutorService.executeBuild(build.getId());

        return build.getId();
    }

    /**
     * 版本回退：从备份文件回退到指定的构建版本
     */
    public Long rollbackToBuild(Long buildId) {
        BuildDO originalBuild = this.getById(buildId);
        if (originalBuild == null) {
            throw new RuntimeException("原始构建记录不存在");
        }

        ProjectDO project = projectService.getById(originalBuild.getProjectId());
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        // 创建新的构建记录（标记为回退操作）
        BuildDO build = new BuildDO();
        build.setProjectId(project.getId());
        build.setStatus("PENDING");
        build.setTriggerBy(StpUtil.getLoginIdAsString());
        this.save(build);

        // 异步执行回退
        buildExecutorService.executeRollback(build.getId(), buildId);

        return build.getId();
    }

    /**
     * 版本回退：根据备份文件名回退
     */
    public Long rollbackToBackup(Long projectId, String backupFileName) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        // 创建新的构建记录（标记为回退操作）
        BuildDO build = new BuildDO();
        build.setProjectId(project.getId());
        build.setStatus("PENDING");
        build.setTriggerBy(StpUtil.getLoginIdAsString());
        this.save(build);

        // 异步执行回退
        buildExecutorService.executeRollbackByBackup(build.getId(), projectId, backupFileName);

        return build.getId();
    }

    /**
     * 获取项目的备份文件列表
     */
    public List<Map<String, Object>> getBackupList(Long projectId) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        List<Map<String, Object>> backupList = new ArrayList<>();

        // 获取项目关联的第一台服务器（假设所有服务器的备份一致）
        List<Long> serverIds = projectServerService.getProjectServerIds(projectId);
        if (serverIds == null || serverIds.isEmpty()) {
            return backupList;
        }

        ServerDO server = serverService.getById(serverIds.get(0));
        if (server == null) {
            return backupList;
        }

        try {
            com.jcraft.jsch.Session session = SshUtils.createSession(
                    server.getHost(), server.getPort(), server.getAccount(),
                    server.getPassword(), server.getPrivateKey()
            );
            session.connect();

            try {
                // 构建部署路径
                String uploadPath = project.getDeployPath();
                if (uploadPath == null || uploadPath.trim().isEmpty()) {
                    uploadPath = "/home/deploy/";
                }
                if (!uploadPath.endsWith("/")) {
                    uploadPath += "/";
                }
                uploadPath += project.getName();

                // 获取备份文件列表（按时间倒序）
                String listCommand = String.format(
                        "cd %s && ls -lt *.jar.bak 2>/dev/null | awk '{if(NR>1) print $9, $6, $7, $8}'",
                        uploadPath
                );

                StringBuilder output = new StringBuilder();
                SshUtils.executeCommand(session, listCommand, msg -> {
                    if (msg != null && !msg.contains("命令执行完成")) {
                        output.append(msg);
                    }
                });

                // 解析输出
                String[] lines = output.toString().split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    String[] parts = line.split("\\s+", 4);
                    if (parts.length >= 4) {
                        Map<String, Object> backup = new HashMap<>();
                        backup.put("fileName", parts[0]);
                        backup.put("date", parts[1] + " " + parts[2] + " " + parts[3]);
                        backupList.add(backup);
                    }
                }
            } finally {
                if (session.isConnected()) {
                    session.disconnect();
                }
            }
        } catch (Exception e) {
            log.error("获取备份列表失败", e);
        }

        return backupList;
    }

    /**
     * 定时任务：每隔1分钟检查超时的构建任务
     * 如果构建任务运行超过10分钟还未完成，则标记为失败
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void checkTimeoutBuilds() {
        log.info("开始检查超时构建任务...");

        // 查询所有状态为 RUNNING 或 PENDING 的构建记录
        List<BuildDO> runningBuilds = this.lambdaQuery()
                .in(BuildDO::getStatus, "RUNNING", "PENDING")
                .list();

        if (runningBuilds.isEmpty()) {
            log.debug("没有正在运行的构建任务");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int timeoutCount = 0;

        for (BuildDO build : runningBuilds) {
            // 获取构建开始时间，如果没有开始时间则使用创建时间
            LocalDateTime checkTime = build.getStartTime() != null ? build.getStartTime() : build.getCreatedAt();

            if (checkTime == null) {
                continue;
            }

            // 检查是否超过10分钟
            if (checkTime.plusMinutes(10).isBefore(now)) {
                log.warn("构建任务 {} 超时，开始时间: {}，当前时间: {}，标记为失败",
                        build.getId(), checkTime, now);

                // 更新状态为失败
                build.setStatus("FAILED");
                build.setEndTime(now);

                // 计算持续时间（秒）
                if (build.getStartTime() != null) {
                    long duration = java.time.Duration.between(build.getStartTime(), now).getSeconds();
                    build.setDuration(duration);
                }

                // 追加超时日志
                String timeoutLog = "\n\n[系统] 构建超时（超过10分钟），已自动标记为失败";
                build.setLog(build.getLog() != null ? build.getLog() + timeoutLog : timeoutLog);

                this.updateById(build);
                timeoutCount++;
            }
        }

        if (timeoutCount > 0) {
            log.info("本次检查发现 {} 个超时构建任务，已标记为失败", timeoutCount);
        }
    }
}
