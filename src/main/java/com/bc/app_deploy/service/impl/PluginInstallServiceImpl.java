package com.bc.app_deploy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.PluginInstallMapper;
import com.bc.app_deploy.model.entity.PluginDO;
import com.bc.app_deploy.model.entity.PluginInstallDO;
import com.bc.app_deploy.service.IPluginExecutorService;
import com.bc.app_deploy.service.IPluginInstallService;
import com.bc.app_deploy.service.IPluginService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
public class PluginInstallServiceImpl extends ServiceImpl<PluginInstallMapper, PluginInstallDO> implements IPluginInstallService {
    
    @Resource
    private IPluginService pluginService;
    
    @Resource
    @Lazy
    private IPluginExecutorService pluginExecutorService;
    
    /**
     * 获取服务器上已安装的插件列表
     */
    public List<PluginInstallDO> getServerPlugins(Long serverId) {
        LambdaQueryWrapper<PluginInstallDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PluginInstallDO::getServerId, serverId)
               .orderByDesc(PluginInstallDO::getCreateTime);
        return this.list(wrapper);
    }
    
    /**
     * 检查插件是否已安装
     */
    public PluginInstallDO getInstalled(Long serverId, Long pluginId) {
        LambdaQueryWrapper<PluginInstallDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PluginInstallDO::getServerId, serverId)
               .eq(PluginInstallDO::getPluginId, pluginId)
               .eq(PluginInstallDO::getStatus, "SUCCESS");
        return this.getOne(wrapper);
    }
    
    /**
     * 触发插件安装
     */
    public Long install(Long serverId, Long pluginId, String version) {
        PluginDO plugin = pluginService.getById(pluginId);
        if (plugin == null) {
            throw new RuntimeException("插件不存在");
        }
        
        // 创建安装记录
        PluginInstallDO install = new PluginInstallDO();
        install.setServerId(serverId);
        install.setPluginId(pluginId);
        install.setVersion(version);
        install.setStatus("PENDING");
        install.setOperateBy(StpUtil.getLoginIdAsString());
        this.save(install);
        
        // 异步执行安装（通过外部Service调用，确保异步生效）
        pluginExecutorService.executeInstall(install.getId());
        
        return install.getId();
    }
    
    /**
     * 触发插件卸载
     */
    public void uninstall(Long installId) {
        PluginInstallDO install = this.getById(installId);
        if (install == null) {
            throw new RuntimeException("安装记录不存在");
        }
        
        // 异步执行卸载（通过外部Service调用，确保异步生效）
        pluginExecutorService.executeUninstall(installId);
    }
}
