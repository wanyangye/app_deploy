package com.bc.app_deploy.controller;

import com.bc.app_deploy.model.entity.PluginDO;
import com.bc.app_deploy.model.entity.PluginInstallDO;
import com.bc.app_deploy.service.IPluginInstallService;
import com.bc.app_deploy.service.IPluginService;

import com.bc.app_deploy.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plugin")
public class PluginController {

    @Resource
    private IPluginService pluginService;

    @Resource
    private IPluginInstallService pluginInstallService;

    /**
     * 获取所有可用插件(按分类分组)
     */
    @GetMapping("/list")
    public Result<Map<String, List<PluginDO>>> list() {
        Map<String, List<PluginDO>> result = new HashMap<>();
        result.put("BASE_ENV", pluginService.getByCategory("BASE_ENV"));
        result.put("MIDDLEWARE", pluginService.getByCategory("MIDDLEWARE"));
        return Result.success(result);
    }

    /**
     * 获取服务器已安装的插件列表
     */
    @GetMapping("/installed/{serverId}")
    public Result<List<PluginInstallDO>> installed(@PathVariable Long serverId) {
        return Result.success(pluginInstallService.getServerPlugins(serverId));
    }

    /**
     * 检查插件是否已安装
     */
    @GetMapping("/check/{serverId}/{pluginId}")
    public Result<PluginInstallDO> checkInstalled(
            @PathVariable Long serverId,
            @PathVariable Long pluginId) {
        PluginInstallDO install = pluginInstallService.getInstalled(serverId, pluginId);
        return Result.success(install);
    }

    /**
     * 安装插件
     */

    @PostMapping("/install")
    public Result<Long> install(@RequestBody Map<String, Object> params) {
        Long serverId = Long.valueOf(params.get("serverId").toString());
        Long pluginId = Long.valueOf(params.get("pluginId").toString());
        String version = params.get("version").toString();

        Long installId = pluginInstallService.install(serverId, pluginId, version);
        return Result.success(installId);
    }

    /**
     * 卸载插件
     */

    @DeleteMapping("/uninstall/{installId}")
    public Result<String> uninstall(@PathVariable Long installId) {
        pluginInstallService.uninstall(installId);
        return Result.success("卸载任务已提交");
    }

    /**
     * 获取安装日志
     */
    @GetMapping("/install/log/{installId}")
    public Result<PluginInstallDO> getInstallLog(@PathVariable Long installId) {
        return Result.success(pluginInstallService.getById(installId));
    }

    /**
     * 管理员 - 插件管理
     */
    @GetMapping("/manage/list")
    public Result<List<PluginDO>> manageList() {
        return Result.success(pluginService.list());
    }


    @PostMapping("/manage")
    public Result<String> savePlugin(@RequestBody PluginDO plugin) {
        pluginService.saveOrUpdate(plugin);
        return Result.success("保存成功");
    }


    @DeleteMapping("/manage/{id}")
    public Result<String> deletePlugin(@PathVariable Long id) {
        pluginService.removeById(id);
        return Result.success("删除成功");
    }
}
