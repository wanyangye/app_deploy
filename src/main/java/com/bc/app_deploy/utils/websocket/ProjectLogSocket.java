package com.bc.app_deploy.utils.websocket;

import com.bc.app_deploy.model.entity.ProjectDO;
import com.bc.app_deploy.model.entity.ServerDO;
import com.bc.app_deploy.service.IProjectServerService;
import com.bc.app_deploy.service.IProjectService;
import com.bc.app_deploy.service.IServerService;
import com.bc.app_deploy.utils.SshUtils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 项目运行日志 WebSocket
 * 通过SSH连接服务器，使用 tail -f 实时查看项目运行日志
 */
@Slf4j
@Component
@ServerEndpoint("/ws/project/log/{projectId}/{serverId}")
public class ProjectLogSocket {

    private static IProjectService projectService;
    private static IServerService serverService;
    private static IProjectServerService projectServerService;

    // 存储所有连接的客户端和对应的SSH会话
    private static final ConcurrentHashMap<String, LogConnection> CONNECTION_MAP = new ConcurrentHashMap<>();

    // 执行器线程池
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    @Autowired
    public void setProjectService(IProjectService projectService) {
        ProjectLogSocket.projectService = projectService;
    }

    @Autowired
    public void setServerService(IServerService serverService) {
        ProjectLogSocket.serverService = serverService;
    }

    @Autowired
    public void setProjectServerService(IProjectServerService projectServerService) {
        ProjectLogSocket.projectServerService = projectServerService;
    }

    @OnOpen
    public void onOpen(jakarta.websocket.Session session,
                       @PathParam("projectId") Long projectId,
                       @PathParam("serverId") Long serverId) {
        String key = projectId + "_" + serverId + "_" + session.getId();
        log.info("项目日志WebSocket连接建立: projectId={}, serverId={}", projectId, serverId);

        // 获取自定义日志路径参数
        String customLogPath = null;
        if (session.getQueryString() != null && session.getQueryString().contains("logPath=")) {
            try {
                String query = session.getQueryString();
                String logPathParam = query.substring(query.indexOf("logPath=") + 8);
                if (logPathParam.contains("&")) {
                    logPathParam = logPathParam.substring(0, logPathParam.indexOf("&"));
                }
                customLogPath = java.net.URLDecoder.decode(logPathParam, "UTF-8");
            } catch (Exception e) {
                log.warn("解析自定义日志路径失败", e);
            }
        }
        final String logPath = customLogPath;

        try {
            // 获取项目信息
            ProjectDO project = projectService.getById(projectId);
            if (project == null) {
                sendMessage(session, "[错误] 项目不存在\n");
                session.close();
                return;
            }

            // 获取服务器信息
            ServerDO server = serverService.getById(serverId);
            if (server == null) {
                sendMessage(session, "[错误] 服务器不存在\n");
                session.close();
                return;
            }

            sendMessage(session, "[系统] 正在连接服务器 " + server.getName() + " ...\n");

            // 异步建立SSH连接并执行tail命令
            EXECUTOR.submit(() -> {
                try {
                    startTailLog(session, key, project, server, logPath);
                } catch (Exception e) {
                    log.error("启动日志监控失败", e);
                    sendMessage(session, "[错误] 连接失败: " + e.getMessage() + "\n");
                }
            });

        } catch (Exception e) {
            log.error("WebSocket连接处理失败", e);
            sendMessage(session, "[错误] 连接处理失败: " + e.getMessage() + "\n");
        }
    }

    /**
     * 启动 tail -f 日志监控
     */
    private void startTailLog(jakarta.websocket.Session wsSession, String key, ProjectDO project, ServerDO server, String customLogPath) {
        Session sshSession = null;
        ChannelExec channel = null;

        try {
            // 创建SSH连接
            sshSession = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            sshSession.connect(10000);
            sendMessage(wsSession, "[系统] SSH连接成功\n");

            // 确定日志文件路径
            String logFile;
            if (customLogPath != null && !customLogPath.isEmpty()) {
                // 使用自定义路径
                String deployPath = project.getDeployPath();
                if (deployPath == null || deployPath.isEmpty()) {
                    deployPath = "/home/deploy/";
                }
                if (!deployPath.endsWith("/")) {
                    deployPath += "/";
                }
                // 如果自定义路径以/开头，则为绝对路径，否则拼接
                if (customLogPath.startsWith("/")) {
                    logFile = customLogPath;
                } else {
                    logFile = deployPath + project.getName() + "/" + customLogPath;
                }
            } else {
                // 默认路径: {deployPath}/{projectName}/app.log
                String deployPath = project.getDeployPath();
                if (deployPath == null || deployPath.isEmpty()) {
                    deployPath = "/home/deploy/";
                }
                if (!deployPath.endsWith("/")) {
                    deployPath += "/";
                }
                logFile = deployPath + project.getName() + "/app.log";
            }

            sendMessage(wsSession, "[系统] 开始监控日志文件: " + logFile + "\n");
            sendMessage(wsSession, "[系统] 按 Ctrl+C 或关闭弹窗停止监控\n");
            sendMessage(wsSession, "========================================\n");

            // 执行 tail -f 命令
            channel = (ChannelExec) sshSession.openChannel("exec");
            String command = "tail -f -n 100 " + logFile + " 2>&1";
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            channel.connect();

            // 保存连接信息
            LogConnection connection = new LogConnection(sshSession, channel);
            CONNECTION_MAP.put(key, connection);

            // 持续读取日志输出
            byte[] buffer = new byte[1024];
            int len;
            while (wsSession.isOpen() && !channel.isClosed()) {
                while (in.available() > 0) {
                    len = in.read(buffer);
                    if (len > 0) {
                        String output = new String(buffer, 0, len);
                        sendMessage(wsSession, output);
                    }
                }
                Thread.sleep(100);
            }

        } catch (Exception e) {
            log.error("日志监控异常", e);
            sendMessage(wsSession, "[错误] " + e.getMessage() + "\n");
        } finally {
            // 清理连接
            closeConnection(key);
        }
    }

    @OnClose
    public void onClose(jakarta.websocket.Session session,
                        @PathParam("projectId") Long projectId,
                        @PathParam("serverId") Long serverId) {
        String key = projectId + "_" + serverId + "_" + session.getId();
        log.info("项目日志WebSocket连接关闭: projectId={}, serverId={}", projectId, serverId);
        closeConnection(key);
    }

    @OnMessage
    public void onMessage(String message,
                          @PathParam("projectId") Long projectId,
                          @PathParam("serverId") Long serverId) {
        // 可以用于接收停止命令等
        log.info("收到客户端消息: projectId={}, serverId={}, message={}", projectId, serverId, message);
    }

    @OnError
    public void onError(jakarta.websocket.Session session, Throwable error,
                        @PathParam("projectId") Long projectId,
                        @PathParam("serverId") Long serverId) {
        String key = projectId + "_" + serverId + "_" + session.getId();
        log.error("项目日志WebSocket错误: projectId={}, serverId={}", projectId, serverId, error);
        closeConnection(key);
    }

    private void sendMessage(jakarta.websocket.Session session, String message) {
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
        }
    }

    private void closeConnection(String key) {
        LogConnection connection = CONNECTION_MAP.remove(key);
        if (connection != null) {
            try {
                if (connection.channel != null && connection.channel.isConnected()) {
                    connection.channel.disconnect();
                }
                if (connection.session != null && connection.session.isConnected()) {
                    connection.session.disconnect();
                }
                log.info("SSH连接已关闭: {}", key);
            } catch (Exception e) {
                log.error("关闭SSH连接失败", e);
            }
        }
    }

    /**
     * 日志连接信息
     */
    private static class LogConnection {
        Session session;
        ChannelExec channel;

        LogConnection(Session session, ChannelExec channel) {
            this.session = session;
            this.channel = channel;
        }
    }
}
