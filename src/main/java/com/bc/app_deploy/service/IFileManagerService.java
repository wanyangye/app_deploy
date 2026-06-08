package com.bc.app_deploy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bc.app_deploy.model.entity.FileManagerDO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface IFileManagerService extends IService<FileManagerDO> {
    List<Map<String, Object>> listFiles(Long serverId, String path);

    Map<String, Object> uploadFile(Long serverId, String remotePath, MultipartFile file);

    Map<String, Object> uploadBatch(Long serverId, String remotePath, List<MultipartFile> files, List<String> paths);

    Map<String, Object> createDirectory(Long serverId, String path, String dirName);

    Map<String, Object> deleteFile(Long serverId, String filePath);

    Map<String, Object> downloadFileStream(Long serverId, String filePath);
}
