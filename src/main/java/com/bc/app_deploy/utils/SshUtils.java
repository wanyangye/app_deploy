package com.bc.app_deploy.utils;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

@Slf4j
public class SshUtils {

    /**
     * 创建SSH会话
     */
    public static Session createSession(String host, int port, String username,
                                        String password, String privateKey) throws Exception {
        JSch jsch = new JSch();

        if (privateKey != null && !privateKey.isEmpty()) {
            // 使用私钥
            jsch.addIdentity("key", privateKey.getBytes(), null, null);
        }

        Session session = jsch.getSession(username, host, port);

        if (password != null && !password.isEmpty()) {
            session.setPassword(password);
        }

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(30000);

        return session;
    }

    /**
     * 测试SSH连接
     */
    public static boolean testConnection(String host, int port, String username,
                                         String password, String privateKey) {
        Session session = null;
        try {
            session = createSession(host, port, username, password, privateKey);
            session.connect();
            return session.isConnected();
        } catch (Exception e) {
            log.error("SSH连接测试失败", e);
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 执行命令
     */
    public static void executeCommand(Session session, String command, Consumer<String> logConsumer) throws Exception {
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);

        channelExec.setInputStream(null);
        channelExec.setErrStream(System.err);

        InputStream in = channelExec.getInputStream();
        channelExec.connect();

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                // 使用 UTF-8 编码解码，解决乱码问题
                String output = new String(tmp, 0, i, java.nio.charset.StandardCharsets.UTF_8);
                if (logConsumer != null) {
                    logConsumer.accept(output);
                }
            }
            if (channelExec.isClosed()) {
                if (in.available() > 0) continue;
                if (logConsumer != null) {
                    logConsumer.accept("命令执行完成，退出码: " + channelExec.getExitStatus());
                }
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception ee) {
            }
        }

        channelExec.disconnect();
    }

    /**
     * 上传文件
     */
    public static void uploadFile(Session session, File localFile, String remotePath,
                                  Consumer<String> logConsumer) throws Exception {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            // 确保远程目录存在
            String[] dirs = remotePath.split("/");
            String currentPath = "";
            for (String dir : dirs) {
                if (dir.isEmpty()) continue;
                currentPath += "/" + dir;
                try {
                    channelSftp.cd(currentPath);
                } catch (SftpException e) {
                    channelSftp.mkdir(currentPath);
                    channelSftp.cd(currentPath);
                }
            }

            if (logConsumer != null) {
                logConsumer.accept("[SFTP] 开始上传文件: " + localFile.getName());
            }

            channelSftp.put(new FileInputStream(localFile), localFile.getName(),
                    ChannelSftp.OVERWRITE);

            if (logConsumer != null) {
                logConsumer.accept("[SFTP] 文件上传成功: " + remotePath + "/" + localFile.getName());
            }
        } finally {
            channelSftp.disconnect();
        }
    }
}
