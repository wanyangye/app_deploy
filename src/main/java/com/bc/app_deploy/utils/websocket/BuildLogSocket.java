package com.bc.app_deploy.utils.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@ServerEndpoint("/ws/build/{buildId}")
public class BuildLogSocket {
    
    // 存储所有连接的客户端
    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();
    
    // 为每个buildId的Session提供独立的锁，避免并发写入冲突
    private static final ConcurrentHashMap<String, Lock> LOCK_MAP = new ConcurrentHashMap<>();
    
    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("buildId") String buildId) {
        SESSION_MAP.put(buildId, session);
        LOCK_MAP.put(buildId, new ReentrantLock());
        log.info("WebSocket连接建立: buildId={}", buildId);
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
    public void onClose(@PathParam("buildId") String buildId) {
        SESSION_MAP.remove(buildId);
        LOCK_MAP.remove(buildId);
        log.info("WebSocket连接关闭: buildId={}", buildId);
    }
    
    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, @PathParam("buildId") String buildId) {
        log.info("收到客户端消息: buildId={}, message={}", buildId, message);
    }
    
    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误", error);
    }
    
    /**
     * 发送消息给指定构建ID的客户端（线程安全）
     */
    public static void sendMessage(String buildId, String message) {
        Session session = SESSION_MAP.get(buildId);
        Lock lock = LOCK_MAP.get(buildId);
        
        if (session != null && session.isOpen() && lock != null) {
            lock.lock();
            try {
                // 双重检查，确保session在获取锁后仍然有效
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                log.error("发送消息失败: buildId={}", buildId, e);
            } finally {
                lock.unlock();
            }
        }
    }
}
