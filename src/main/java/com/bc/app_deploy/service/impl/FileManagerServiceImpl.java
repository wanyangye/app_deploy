package com.bc.app_deploy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.FileManagerMapper;
import com.bc.app_deploy.model.entity.FileManagerDO;
import com.bc.app_deploy.model.entity.ServerDO;
import com.bc.app_deploy.service.IFileManagerService;
import com.bc.app_deploy.service.IServerService;
import com.bc.app_deploy.utils.SshUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class FileManagerServiceImpl extends ServiceImpl<FileManagerMapper, FileManagerDO> implements IFileManagerService {
    @Resource
    private IServerService serverService;

    /**
     * 列出目录内容
     */
    public List<Map<String, Object>> listFiles(Long serverId, String path) {
        ServerDO server = serverService.getById(serverId);
        if (server == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> files = new ArrayList<>();
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> fileList = sftpChannel.ls(path);

            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                if (".".equals(filename) || "..".equals(filename)) {
                    continue;
                }

                SftpATTRS attrs = entry.getAttrs();
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", filename);
                fileInfo.put("isDirectory", attrs.isDir());
                fileInfo.put("size", attrs.getSize());
                fileInfo.put("permissions", attrs.getPermissionsString());
                fileInfo.put("modifiedTime", new Date(attrs.getMTime() * 1000L));
                fileInfo.put("path", path.endsWith("/") ? path + filename : path + "/" + filename);

                files.add(fileInfo);
            }

            // 排序：目录在前，文件在后
            files.sort((a, b) -> {
                boolean aIsDir = (boolean) a.get("isDirectory");
                boolean bIsDir = (boolean) b.get("isDirectory");
                if (aIsDir == bIsDir) {
                    return ((String) a.get("name")).compareTo((String) b.get("name"));
                }
                return aIsDir ? -1 : 1;
            });

        } catch (Exception e) {
            log.error("列出目录失败", e);
            throw new RuntimeException("列出目录失败: " + e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return files;
    }

    /**
     * 上传文件
     */
    public Map<String, Object> uploadFile(Long serverId, String remotePath, MultipartFile file) {
        ServerDO server = serverService.getById(serverId);
        if (server == null) {
            throw new RuntimeException("服务器不存在");
        }

        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            String fullPath = remotePath.endsWith("/")
                    ? remotePath + file.getOriginalFilename()
                    : remotePath + "/" + file.getOriginalFilename();

            sftpChannel.put(file.getInputStream(), fullPath);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文件上传成功");
            result.put("filename", file.getOriginalFilename());
            return result;

        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 批量上传文件（支持文件夹结构）
     */
    public Map<String, Object> uploadBatch(Long serverId, String remotePath, List<MultipartFile> files, List<String> paths) {
        ServerDO server = serverService.getById(serverId);
        if (server == null) {
            throw new RuntimeException("服务器不存在");
        }

        Session session = null;
        ChannelSftp sftpChannel = null;
        int successCount = 0;
        int failCount = 0;
        List<String> failedFiles = new ArrayList<>();

        try {
            session = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            // 遍历所有文件
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String relativePath = paths.get(i);

                try {
                    // 构建完整路径
                    String fullPath = remotePath.endsWith("/")
                            ? remotePath + relativePath
                            : remotePath + "/" + relativePath;

                    // 创建目录（如果需要）
                    String dirPath = fullPath.substring(0, fullPath.lastIndexOf('/'));
                    createDirectoryRecursive(sftpChannel, dirPath);

                    // 上传文件
                    sftpChannel.put(file.getInputStream(), fullPath);
                    successCount++;

                    log.info("文件上传成功: {}", fullPath);
                } catch (Exception e) {
                    failCount++;
                    failedFiles.add(relativePath);
                    log.error("上传文件失败: {}", relativePath, e);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", failCount == 0);
            result.put("message", String.format("上传完成: 成功 %d 个, 失败 %d 个", successCount, failCount));
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("failedFiles", failedFiles);
            return result;

        } catch (Exception e) {
            log.error("批量上传文件失败", e);
            throw new RuntimeException("批量上传文件失败: " + e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 递归创建目录
     */
    private void createDirectoryRecursive(ChannelSftp sftpChannel, String path) throws Exception {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return;
        }

        try {
            sftpChannel.stat(path);
            // 目录已存在
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                // 目录不存在，递归创建父目录
                String parentPath = path.substring(0, Math.max(path.lastIndexOf('/'), 0));
                if (!parentPath.isEmpty() && !parentPath.equals("/")) {
                    createDirectoryRecursive(sftpChannel, parentPath);
                }
                // 创建当前目录
                sftpChannel.mkdir(path);
                log.info("创建目录: {}", path);
            } else {
                throw e;
            }
        }
    }

    /**
     * 下载文件（流式传输，适用于大文件）
     * 返回包含文件流和文件大小的Map
     */
    public Map<String, Object> downloadFileStream(Long serverId, String filePath) {
        ServerDO server = serverService.getById(serverId);
        if (server == null) {
            throw new RuntimeException("服务器不存在");
        }

        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            // 检查文件是否存在并获取文件大小
            SftpATTRS attrs;
            try {
                attrs = sftpChannel.stat(filePath);
                if (attrs.isDir()) {
                    throw new RuntimeException("不能下载目录，请选择文件");
                }
            } catch (com.jcraft.jsch.SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    throw new RuntimeException("文件不存在: " + filePath);
                }
                throw e;
            }

            // 获取文件大小
            long fileSize = attrs.getSize();

            // 返回文件流，由调用者负责关闭 session 和 channel
            InputStream inputStream = sftpChannel.get(filePath);

            // 将 session 和 channel 包装到流中，关闭流时同时关闭连接
            final Session finalSession = session;
            final ChannelSftp finalChannel = sftpChannel;

            InputStream wrappedStream = new java.io.FilterInputStream(inputStream) {
                @Override
                public void close() throws java.io.IOException {
                    try {
                        super.close();
                    } finally {
                        if (finalChannel != null && finalChannel.isConnected()) {
                            finalChannel.disconnect();
                        }
                        if (finalSession != null && finalSession.isConnected()) {
                            finalSession.disconnect();
                        }
                    }
                }
            };

            Map<String, Object> result = new HashMap<>();
            result.put("stream", wrappedStream);
            result.put("size", fileSize);
            return result;

        } catch (Exception e) {
            // 发生异常时关闭连接
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            log.error("下载文件失败: serverId={}, filePath={}", serverId, filePath, e);
            throw new RuntimeException("下载文件失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件（旧版本，保留兼容）
     */
    @Deprecated
    public byte[] downloadFile(Long serverId, String filePath) {
        ServerDO server = serverService.getById(serverId);
        if (server == null) {
            throw new RuntimeException("服务器不存在");
        }

        Session session = null;
        ChannelSftp sftpChannel = null;
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;

        try {
            session = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            // 检查文件是否存在
            try {
                SftpATTRS attrs = sftpChannel.stat(filePath);
                if (attrs.isDir()) {
                    throw new RuntimeException("不能下载目录，请选择文件");
                }
            } catch (com.jcraft.jsch.SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    throw new RuntimeException("文件不存在: " + filePath);
                }
                throw e;
            }

            inputStream = sftpChannel.get(filePath);
            outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("下载文件失败: serverId={}, filePath={}", serverId, filePath, e);
            throw new RuntimeException("下载文件失败: " + e.getMessage());
        } finally {
            // 关闭输入流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    log.warn("关闭输入流失败", e);
                }
            }
            // 关闭输出流
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    log.warn("关闭输出流失败", e);
                }
            }
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 删除文件或目录
     */
    public Map<String, Object> deleteFile(Long serverId, String filePath) {
        ServerDO server = serverService.getById(serverId);
        if (server == null) {
            throw new RuntimeException("服务器不存在");
        }

        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            SftpATTRS attrs = sftpChannel.stat(filePath);
            if (attrs.isDir()) {
                // 递归删除目录
                deleteDirectoryRecursive(sftpChannel, filePath);
            } else {
                // 删除文件
                sftpChannel.rm(filePath);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "删除成功");
            return result;

        } catch (Exception e) {
            log.error("删除文件失败: {}", filePath, e);
            throw new RuntimeException("删除失败: " + e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 递归删除目录（包括非空目录）
     */
    private void deleteDirectoryRecursive(ChannelSftp sftpChannel, String path) throws Exception {
        // 列出目录下的所有文件
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> fileList = sftpChannel.ls(path);

        for (ChannelSftp.LsEntry entry : fileList) {
            String filename = entry.getFilename();
            // 跳过 . 和 ..
            if (".".equals(filename) || "..".equals(filename)) {
                continue;
            }

            String fullPath = path.endsWith("/") ? path + filename : path + "/" + filename;
            SftpATTRS attrs = entry.getAttrs();

            if (attrs.isDir()) {
                // 递归删除子目录
                deleteDirectoryRecursive(sftpChannel, fullPath);
            } else {
                // 删除文件
                sftpChannel.rm(fullPath);
                log.debug("已删除文件: {}", fullPath);
            }
        }

        // 所有子文件和子目录删除后，删除当前目录
        sftpChannel.rmdir(path);
        log.info("已删除目录: {}", path);
    }

    /**
     * 创建目录
     */
    public Map<String, Object> createDirectory(Long serverId, String path, String dirName) {
        ServerDO server = serverService.getById(serverId);
        if (server == null) {
            throw new RuntimeException("服务器不存在");
        }

        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            session.connect();

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            String fullPath = path.endsWith("/") ? path + dirName : path + "/" + dirName;
            sftpChannel.mkdir(fullPath);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "创建目录成功");
            result.put("path", fullPath);
            return result;

        } catch (Exception e) {
            log.error("创建目录失败", e);
            throw new RuntimeException("创建目录失败: " + e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
