package com.bc.app_deploy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bc.app_deploy.model.entity.BuildDO;

import java.util.List;
import java.util.Map;

public interface IBuildService extends IService<BuildDO> {
    Long triggerBuild(Long projectId);

    Long rollbackToBackup(Long projectId, String backupFileName);

    List<Map<String, Object>> getBackupList(Long projectId);
}