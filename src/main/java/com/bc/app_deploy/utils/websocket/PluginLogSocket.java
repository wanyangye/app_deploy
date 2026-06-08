package com.bc.app_deploy.utils.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/plugin/{installId}")
public class PluginLogSocket {
    
    // 存储所有连接的客户端
    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();
    
    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("installId") String installId) {
        SESSION_MAP.put(installId, session);
        log.info("WebSocket连接建立: installId={}", installId);
        try {
            session.getBasicRemote().sendText("[系统] 日志连接已建立");
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }
    
    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("installId") String installId) {
        SESSION_MAP.remove(installId);
        log.info("WebSocket连接关闭: installId={}", installId);
    }
    
    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, @PathParam("installId") String installId) {
        log.info("收到客户端消息: installId={}, message={}", installId, message);
    }
    
    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误", error);
    }
    
    /**
     * 发送消息给指定安装ID的客户端
     */
    public static void sendMessage(String installId, String message) {
        Session session = SESSION_MAP.get(installId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("发送消息失败: installId={}", installId, e);
            }
        }
    }
}
