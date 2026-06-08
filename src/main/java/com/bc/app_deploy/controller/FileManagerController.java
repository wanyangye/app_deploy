package com.bc.app_deploy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;

import com.bc.app_deploy.service.IFileManagerService;
import com.bc.app_deploy.utils.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 文件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@SaCheckLogin
public class FileManagerController {

    @Resource
    private IFileManagerService fileManagerService;

    /**
     * 列出目录内容
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listFiles(
            @RequestParam Long serverId,
            @RequestParam(required = false, defaultValue = "/") String path) {
        return Result.success(fileManagerService.listFiles(serverId, path));
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(
            @RequestParam Long serverId,
            @RequestParam String remotePath,
            @RequestParam("file") MultipartFile file) {
        return Result.success(fileManagerService.uploadFile(serverId, remotePath, file));
    }

    /**
     * 批量上传文件（支持文件夹）
     */
    @PostMapping("/upload-batch")
    public Result<Map<String, Object>> uploadBatch(
            @RequestParam Long serverId,
            @RequestParam String remotePath,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("paths") List<String> paths) {
        return Result.success(fileManagerService.uploadBatch(serverId, remotePath, files, paths));
    }

    /**
     * 下载文件
     */
    @GetMapping("/download")
    public void downloadFile(
            @RequestParam Long serverId,
            @RequestParam String filePath,
            HttpServletResponse response) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            // 获取文件流和文件大小
            Map<String, Object> fileData = fileManagerService.downloadFileStream(serverId, filePath);
            inputStream = (InputStream) fileData.get("stream");
            Long fileSize = (Long) fileData.get("size");
            
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            
            // 处理中文文件名编码
            String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            
            // 设置响应头，包括文件大小（重要！用于计算进度）
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setContentLengthLong(fileSize); // 设置文件大小，前端才能计算进度
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            // 流式传输文件，使用更大的缓冲区提高效率
            outputStream = response.getOutputStream();
            byte[] buffer = new byte[8192]; // 8KB缓冲区
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush(); // 确保数据实时传输
            }
            
            log.info("文件下载成功: serverId={}, filePath={}, size={}", serverId, filePath, fileSize);
            
        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            try {
                // 设置错误响应
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setHeader("X-Error-Message", e.getMessage());
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("返回错误信息失败", ex);
            }
        } finally {
            // 关闭流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    log.warn("关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    log.warn("关闭输出流失败", e);
                }
            }
        }
    }

    /**
     * 删除文件或目录
     */
    @DeleteMapping("/delete")
    public Result<Map<String, Object>> deleteFile(
            @RequestParam Long serverId,
            @RequestParam String filePath) {
        return Result.success(fileManagerService.deleteFile(serverId, filePath));
    }

    /**
     * 创建目录
     */
    @PostMapping("/mkdir")
    public Result<Map<String, Object>> createDirectory(
            @RequestParam Long serverId,
            @RequestParam String path,
            @RequestParam String dirName) {
        return Result.success(fileManagerService.createDirectory(serverId, path, dirName));
    }
}
