package com.bc.app_deploy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bc.app_deploy.model.entity.PluginDO;

import java.util.List;

public interface IPluginService extends IService<PluginDO> {
    List<PluginDO> getByCategory(String baseEnv);

    List<PluginDO> getAllEnabled();
}
