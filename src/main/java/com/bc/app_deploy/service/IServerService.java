package com.bc.app_deploy.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.bc.app_deploy.model.entity.ServerDO;

import java.util.Map;

public interface IServerService extends IService<ServerDO> {
    boolean saveOrUpdateServer(ServerDO server);

    boolean hasRelatedProjects(Long serverId);

    boolean testConnection(ServerDO server);

    Map<String, Object> getMonitorInfo(ServerDO server) throws Exception;
}
