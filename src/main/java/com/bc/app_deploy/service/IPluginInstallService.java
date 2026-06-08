package com.bc.app_deploy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bc.app_deploy.model.entity.PluginInstallDO;

import java.util.List;

public interface IPluginInstallService extends IService<PluginInstallDO> {
    List<PluginInstallDO> getServerPlugins(Long serverId);

    PluginInstallDO getInstalled(Long serverId, Long pluginId);

    Long install(Long serverId, Long pluginId, String version);

    void uninstall(Long installId);
}
