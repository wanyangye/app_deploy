package com.bc.app_deploy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bc.app_deploy.model.entity.ProjectServerDO;

import java.util.List;

public interface IProjectServerService extends IService<ProjectServerDO> {
    List<Long> getProjectServerIds(Long id);

    void assignServers(Long id, List<Long> serverIds);
}
