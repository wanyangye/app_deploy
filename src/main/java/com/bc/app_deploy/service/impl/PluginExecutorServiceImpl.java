package com.bc.app_deploy.service.impl;


import com.bc.app_deploy.model.entity.PluginDO;
import com.bc.app_deploy.model.entity.PluginInstallDO;
import com.bc.app_deploy.model.entity.ServerDO;
import com.bc.app_deploy.service.IPluginExecutorService;
import com.bc.app_deploy.service.IPluginInstallService;
import com.bc.app_deploy.service.IPluginService;
import com.bc.app_deploy.service.IServerService;
import com.bc.app_deploy.utils.SshUtils;
import com.bc.app_deploy.utils.websocket.PluginLogSocket;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * 插件安装执行器服务
 * 专门用于异步执行插件安装任务
 */
@Slf4j
@Service
public class PluginExecutorServiceImpl implements IPluginExecutorService {

    @Resource
    private IPluginInstallService pluginInstallService;

    @Resource
    private IPluginService pluginService;

    @Resource
    private IServerService serverService;

    /**
     * 插件安装超时时间(分钟)
     */
    private static final int INSTALL_TIMEOUT_MINUTES = 30;

    /**
     * 异步执行插件安装(带超时控制)
     */
    @Async("buildExecutor")
    public void executeInstall(Long installId) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        PluginInstallDO install = pluginInstallService.getById(installId);
        PluginDO plugin = pluginService.getById(install.getPluginId());
        ServerDO server = serverService.getById(install.getServerId());

        // 更新状态为安装中
        install.setStatus("INSTALLING");
        install.setInstallTime(LocalDateTime.now());
        pluginInstallService.updateById(install);

        StringBuilder logBuilder = new StringBuilder();

        try {
            // 日志输出回调
            Consumer<String> logConsumer = msg -> {
                logBuilder.append(msg).append("\n");
                PluginLogSocket.sendMessage(installId.toString(), msg);
                log.info("[Plugin-{}] {}", installId, msg);
            };

            logConsumer.accept("========================================");
            logConsumer.accept("开始安装插件: " + plugin.getName() + " (" + install.getVersion() + ")");
            logConsumer.accept("目标服务器: " + server.getHost());
            logConsumer.accept("========================================");

            // 创建SSH连接
            logConsumer.accept("\n[SSH] 连接服务器: " + server.getHost());
            Session session = null;

            try {
                session = SshUtils.createSession(
                        server.getHost(),
                        server.getPort(),
                        server.getAccount(),
                        server.getPassword(),
                        server.getPrivateKey()
                );

                // 设置连接超时
                session.setTimeout(INSTALL_TIMEOUT_MINUTES * 60 * 1000);
                session.connect();
                logConsumer.accept("[SSH] 连接成功");

                // 执行安装命令
                String script = plugin.getInstallScript()
                        .replace("{{version}}", install.getVersion());

                logConsumer.accept("\n[安装] 执行安装命令: " + script);

                // 检查是否超时
                checkTimeout(startTime, INSTALL_TIMEOUT_MINUTES);

                executeCommand(session, script, logConsumer);

                // 安装成功
                logConsumer.accept("\n========================================");
                logConsumer.accept("安装成功！");
                logConsumer.accept("========================================");

                install.setStatus("SUCCESS");

            } finally {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                    logConsumer.accept("[SSH] 连接已关闭");
                }
            }

        } catch (Exception e) {
            log.error("安装失败", e);
            String errorMsg;
            if (e instanceof java.util.concurrent.TimeoutException) {
                errorMsg = "[错误] 安装超时,已超过 " + INSTALL_TIMEOUT_MINUTES + " 分钟";
            } else {
                errorMsg = "[错误] " + e.getMessage();
            }
            logBuilder.append("\n").append(errorMsg).append("\n");
            PluginLogSocket.sendMessage(installId.toString(), errorMsg);

            install.setStatus("FAILED");
        } finally {
            // 更新安装记录
            install.setLog(logBuilder.toString());
            pluginInstallService.updateById(install);
        }
    }

    /**
     * 异步执行插件卸载
     */
    @Async("buildExecutor")
    public void executeUninstall(Long installId) {
        PluginInstallDO install = pluginInstallService.getById(installId);
        PluginDO plugin = pluginService.getById(install.getPluginId());
        ServerDO server = serverService.getById(install.getServerId());

        // 更新状态为卸载中
        install.setStatus("UNINSTALLING");
        pluginInstallService.updateById(install);

        StringBuilder logBuilder = new StringBuilder();

        try {
            // 日志输出回调
            Consumer<String> logConsumer = msg -> {
                logBuilder.append(msg).append("\n");
                PluginLogSocket.sendMessage(installId.toString(), msg);
                log.info("[Plugin-{}] {}", installId, msg);
            };

            logConsumer.accept("========================================");
            logConsumer.accept("开始卸载插件: " + plugin.getName());
            logConsumer.accept("目标服务器: " + server.getHost());
            logConsumer.accept("========================================");

            // 创建SSH连接
            logConsumer.accept("\n[SSH] 连接服务器: " + server.getHost());
            Session session = null;

            try {
                session = SshUtils.createSession(
                        server.getHost(),
                        server.getPort(),
                        server.getAccount(),
                        server.getPassword(),
                        server.getPrivateKey()
                );
                session.connect();
                logConsumer.accept("[SSH] 连接成功");

                // 执行卸载命令
                String script = plugin.getUninstallScript();
                logConsumer.accept("\n[卸载] 执行卸载命令: " + script);
                executeCommand(session, script, logConsumer);

                // 卸载成功，删除安装记录
                logConsumer.accept("\n========================================");
                logConsumer.accept("卸载成功！");
                logConsumer.accept("========================================");

                pluginInstallService.removeById(installId);
                return; // 已删除记录，不需要再更新

            } finally {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                    logConsumer.accept("[SSH] 连接已关闭");
                }
            }

        } catch (Exception e) {
            log.error("卸载失败", e);
            String errorMsg = "[错误] " + e.getMessage();
            logBuilder.append("\n").append(errorMsg).append("\n");
            PluginLogSocket.sendMessage(installId.toString(), errorMsg);

            install.setStatus("FAILED");
            install.setLog(logBuilder.toString());
            pluginInstallService.updateById(install);
        }
    }

    /**
     * 执行命令并实时输出日志
     */
    private void executeCommand(Session session, String command, Consumer<String> logConsumer) throws Exception {
        // 判断是否为多行脚本
        boolean isMultiLineScript = isShellScript(command);

        if (isMultiLineScript) {
            executeShellScript(session, command, logConsumer);
        } else {
            executeSingleCommand(session, command, logConsumer);
        }
    }

    /**
     * 判断是否为多行 shell 脚本
     */
    private boolean isShellScript(String command) {
        if (command == null) {
            return false;
        }
        // 包含换行符、以 shebang 开头、或包含多个命令行
        return command.contains("\n")
                || command.trim().startsWith("#!/")
                || command.contains("\r\n");
    }

    /**
     * 执行单行命令
     */
    private void executeSingleCommand(Session session, String command, Consumer<String> logConsumer) throws Exception {
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ChannelExec execChannel = (ChannelExec) channel;

            // 使用 bash -c 执行命令
            execChannel.setCommand("/bin/bash -c \"" + command.replace("\"", "\\\"") + "\"");

            // 获取输出流
            InputStream in = execChannel.getInputStream();
            InputStream err = execChannel.getErrStream();

            execChannel.connect();

            // 读取标准输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(err));

            String line;
            while ((line = reader.readLine()) != null) {
                logConsumer.accept(line);
            }

            // 读取错误输出
            while ((line = errReader.readLine()) != null) {
                logConsumer.accept("[STDERR] " + line);
            }

            // 等待命令执行完成
            while (!execChannel.isClosed()) {
                Thread.sleep(100);
            }

            // 检查退出码
            int exitStatus = execChannel.getExitStatus();
            if (exitStatus != 0) {
                throw new RuntimeException("命令执行失败，退出码: " + exitStatus);
            }

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    /**
     * 执行多行 shell 脚本
     * 将脚本写入服务器临时文件，然后执行
     */
    private void executeShellScript(Session session, String script, Consumer<String> logConsumer) throws Exception {
        // 生成唯一的脚本文件名
        String scriptFileName = "/tmp/app_plugin_" + System.currentTimeMillis() + ".sh";

        logConsumer.accept("[脚本] 检测到多行脚本，准备执行...");

        try {
            // 1. 规范化脚本内容（处理 Windows 换行符）
            String normalizedScript = normalizeScript(script);

            // 2. 将脚本写入服务器临时文件
            writeScriptToRemote(session, normalizedScript, scriptFileName, logConsumer);

            // 3. 设置脚本可执行权限
            executeSimpleCommand(session, "chmod +x " + scriptFileName);
            logConsumer.accept("[脚本] 已设置执行权限");

            // 4. 执行脚本
            logConsumer.accept("[脚本] 开始执行...");
            logConsumer.accept("----------------------------------------");

            Channel channel = null;
            try {
                channel = session.openChannel("exec");
                ChannelExec execChannel = (ChannelExec) channel;

                // 使用 bash 执行脚本文件
                execChannel.setCommand("/bin/bash " + scriptFileName);

                // 获取输出流
                InputStream in = execChannel.getInputStream();
                InputStream err = execChannel.getErrStream();

                execChannel.connect();

                // 并行读取标准输出和错误输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                BufferedReader errReader = new BufferedReader(new InputStreamReader(err));

                String line;
                while ((line = reader.readLine()) != null) {
                    logConsumer.accept(line);
                }

                // 读取错误输出
                while ((line = errReader.readLine()) != null) {
                    logConsumer.accept("[STDERR] " + line);
                }

                // 等待命令执行完成
                while (!execChannel.isClosed()) {
                    Thread.sleep(100);
                }

                logConsumer.accept("----------------------------------------");

                // 检查退出码
                int exitStatus = execChannel.getExitStatus();
                if (exitStatus != 0) {
                    throw new RuntimeException("脚本执行失败，退出码: " + exitStatus);
                }

                logConsumer.accept("[脚本] 执行成功");

            } finally {
                if (channel != null && channel.isConnected()) {
                    channel.disconnect();
                }
            }

        } finally {
            // 5. 清理临时脚本文件
            try {
                executeSimpleCommand(session, "rm -f " + scriptFileName);
                logConsumer.accept("[脚本] 已清理临时文件");
            } catch (Exception e) {
                log.warn("清理临时脚本文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 规范化脚本内容
     * 处理 Windows 换行符，确保脚本在 Linux 上正常执行
     */
    private String normalizeScript(String script) {
        if (script == null) {
            return "";
        }
        // 将 Windows 换行符转换为 Unix 换行符
        String normalized = script.replace("\r\n", "\n").replace("\r", "\n");

        // 确保脚本以换行符结尾
        if (!normalized.endsWith("\n")) {
            normalized += "\n";
        }

        // 如果脚本没有 shebang，添加一个
        if (!normalized.trim().startsWith("#!")) {
            normalized = "#!/bin/bash\n" + normalized;
        }

        return normalized;
    }

    /**
     * 将脚本写入远程服务器
     */
    private void writeScriptToRemote(Session session, String script, String remoteFilePath, Consumer<String> logConsumer) throws Exception {
        // 使用 cat 和 heredoc 将脚本写入文件
        // 使用 base64 编码避免特殊字符问题
        String base64Script = java.util.Base64.getEncoder().encodeToString(script.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String writeCommand = "echo '" + base64Script + "' | base64 -d > " + remoteFilePath;

        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ChannelExec execChannel = (ChannelExec) channel;

            execChannel.setCommand(writeCommand);
            execChannel.connect();

            // 等待命令执行完成
            while (!execChannel.isClosed()) {
                Thread.sleep(100);
            }

            int exitStatus = execChannel.getExitStatus();
            if (exitStatus != 0) {
                throw new RuntimeException("写入脚本文件失败，退出码: " + exitStatus);
            }

            logConsumer.accept("[脚本] 已上传脚本到服务器: " + remoteFilePath);

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    /**
     * 执行简单命令（不关心输出）
     */
    private void executeSimpleCommand(Session session, String command) throws Exception {
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ChannelExec execChannel = (ChannelExec) channel;

            execChannel.setCommand(command);
            execChannel.connect();

            // 等待命令执行完成
            while (!execChannel.isClosed()) {
                Thread.sleep(100);
            }
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    /**
     * 检查是否超时
     */
    private void checkTimeout(long startTime, int timeoutMinutes) throws java.util.concurrent.TimeoutException {
        long elapsed = System.currentTimeMillis() - startTime;
        long timeoutMillis = timeoutMinutes * 60 * 1000L;

        if (elapsed > timeoutMillis) {
            throw new java.util.concurrent.TimeoutException("操作超时,已超过 " + timeoutMinutes + " 分钟");
        }
    }
}
