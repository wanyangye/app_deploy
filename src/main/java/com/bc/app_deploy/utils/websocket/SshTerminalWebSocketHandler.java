package com.bc.app_deploy.utils.websocket;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSH 终端 WebSocket 处理器
 */
@Slf4j
@Component
public class SshTerminalWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, SshConnection> connections = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket SSH 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        // 解析消息：格式为 "CONNECT|host|port|username|password" 或普通命令
        if (payload.startsWith("CONNECT|")) {
            handleConnect(session, payload);
        } else if (payload.equals("CLOSE")) {
            handleClose(session);
        } else {
            handleCommand(session, payload);
        }
    }

    /**
     * 处理连接请求
     */
    private void handleConnect(WebSocketSession session, String payload) {
        try {
            String[] parts = payload.split("\\|");
            if (parts.length < 5) {
                session.sendMessage(new TextMessage("错误：连接参数不完整\r\n"));
                return;
            }

            String host = parts[1];
            int port = Integer.parseInt(parts[2]);
            String username = parts[3];
            String authType = parts[4];
            String credential = parts.length > 5 ? parts[5] : "";

            // 创建 SSH 连接
            JSch jsch = new JSch();
            Session sshSession = jsch.getSession(username, host, port);
            
            if ("PASSWORD".equals(authType)) {
                sshSession.setPassword(credential);
            } else if ("SSH_KEY".equals(authType)) {
                jsch.addIdentity("key", credential.getBytes(), null, null);
            }
            
            sshSession.setConfig("StrictHostKeyChecking", "no");
            sshSession.connect(10000);

            // 打开 shell 通道
            ChannelShell channel = (ChannelShell) sshSession.openChannel("shell");
            channel.setPtyType("xterm");
            channel.setPtySize(80, 24, 640, 480);

            InputStream inputStream = channel.getInputStream();
            OutputStream outputStream = channel.getOutputStream();

            channel.connect();

            // 保存连接
            SshConnection connection = new SshConnection(sshSession, channel, inputStream, outputStream);
            connections.put(session.getId(), connection);

            // 启动输出读取线程
            executorService.submit(() -> readOutput(session, connection));

            session.sendMessage(new TextMessage("连接成功\r\n"));
            log.info("SSH 连接成功: {}@{}:{}", username, host, port);

        } catch (Exception e) {
            log.error("SSH 连接失败", e);
            try {
                session.sendMessage(new TextMessage("连接失败: " + e.getMessage() + "\r\n"));
            } catch (IOException ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }

    /**
     * 处理命令输入
     */
    private void handleCommand(WebSocketSession session, String command) {
        SshConnection connection = connections.get(session.getId());
        if (connection == null) {
            try {
                session.sendMessage(new TextMessage("错误：未建立 SSH 连接\r\n"));
            } catch (IOException e) {
                log.error("发送错误消息失败", e);
            }
            return;
        }

        try {
            connection.getOutputStream().write(command.getBytes());
            connection.getOutputStream().flush();
        } catch (IOException e) {
            log.error("发送命令失败", e);
            try {
                session.sendMessage(new TextMessage("错误：发送命令失败\r\n"));
            } catch (IOException ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }

    /**
     * 读取 SSH 输出
     */
    private void readOutput(WebSocketSession session, SshConnection connection) {
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = connection.getInputStream().read(buffer)) != -1) {
                if (session.isOpen()) {
                    String output = new String(buffer, 0, len);
                    session.sendMessage(new TextMessage(output));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("读取 SSH 输出失败", e);
        }
    }

    /**
     * 处理关闭请求
     */
    private void handleClose(WebSocketSession session) {
        closeConnection(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket SSH 连接关闭: {}, 状态: {}", session.getId(), status);
        closeConnection(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket SSH 传输错误: {}", session.getId(), exception);
        closeConnection(session.getId());
    }

    /**
     * 关闭 SSH 连接
     */
    private void closeConnection(String sessionId) {
        SshConnection connection = connections.remove(sessionId);
        if (connection != null) {
            try {
                if (connection.getChannel() != null && connection.getChannel().isConnected()) {
                    connection.getChannel().disconnect();
                }
                if (connection.getSession() != null && connection.getSession().isConnected()) {
                    connection.getSession().disconnect();
                }
                log.info("SSH 连接已关闭: {}", sessionId);
            } catch (Exception e) {
                log.error("关闭 SSH 连接失败", e);
            }
        }
    }

    /**
     * SSH 连接信息
     */
    private static class SshConnection {
        private final Session session;
        private final ChannelShell channel;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SshConnection(Session session, ChannelShell channel, InputStream inputStream, OutputStream outputStream) {
            this.session = session;
            this.channel = channel;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        public Session getSession() {
            return session;
        }

        public ChannelShell getChannel() {
            return channel;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public OutputStream getOutputStream() {
            return outputStream;
        }
    }
}
