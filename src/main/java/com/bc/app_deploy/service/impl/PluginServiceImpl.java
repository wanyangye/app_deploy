package com.bc.app_deploy.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.PluginMapper;
import com.bc.app_deploy.model.entity.PluginDO;
import com.bc.app_deploy.service.IPluginService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PluginServiceImpl extends ServiceImpl<PluginMapper, PluginDO> implements IPluginService {

    /**
     * 根据分类获取插件列表
     */
    public List<PluginDO> getByCategory(String category) {
        LambdaQueryWrapper<PluginDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PluginDO::getCategory, category)
                .eq(PluginDO::getStatus, 1)
                .orderByAsc(PluginDO::getSort);
        return this.list(wrapper);
    }

    /**
     * 获取所有可用插件
     */
    public List<PluginDO> getAllEnabled() {
        LambdaQueryWrapper<PluginDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PluginDO::getStatus, 1)
                .orderByAsc(PluginDO::getCategory, PluginDO::getSort);
        return this.list(wrapper);
    }
}
