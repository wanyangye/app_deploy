package com.bc.app_deploy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bc.app_deploy.model.entity.ServerDO;
import com.bc.app_deploy.service.system.IProjectServerService;
import com.bc.app_deploy.service.system.IServerService;
import com.bc.app_deploy.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/server")
public class ServerController {

    @Autowired
    private IServerService serverService;

    @Autowired
    private IProjectServerService projectServerService;

    @GetMapping("/list")
    public Result<Page<ServerDO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        Page<ServerDO> page = new Page<>(current, size);
        LambdaQueryWrapper<ServerDO> wrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            wrapper.like(ServerDO::getName, name);
        }

        wrapper.orderByDesc(ServerDO::getCreatedAt);

        return Result.success(serverService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public Result<ServerDO> getById(@PathVariable Long id) {
        return Result.success(serverService.getById(id));
    }


    @PostMapping
    public Result<String> save(@RequestBody ServerDO server) {
        serverService.saveOrUpdateServer(server);
        return Result.success("添加成功");
    }


    @PutMapping
    public Result<String> update(@RequestBody ServerDO server) {
        serverService.saveOrUpdateServer(server);
        return Result.success("更新成功");
    }


    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        // 检查服务器是否关联了项目
        boolean hasProjects = serverService.hasRelatedProjects(id);
        if (hasProjects) {
            return Result.error("该服务器已关联项目，无法删除");
        }

        serverService.removeById(id);
        return Result.success("删除成功");
    }

    @PostMapping("/test/{id}")
    public Result<String> testConnection(@PathVariable Long id) {
        ServerDO server = serverService.getById(id);
        if (server == null) {
            return Result.error("服务器不存在");
        }

        boolean connected = serverService.testConnection(server);
        return connected ? Result.success("连接成功") : Result.error("连接失败");
    }

    @GetMapping("/detail/{id}")
    public Result<ServerDO> getDetail(@PathVariable Long id) {
        ServerDO server = serverService.getById(id);
        if (server == null) {
            return Result.error("服务器不存在");
        }
        return Result.success(server);
    }

    /**
     * 获取服务器监控信息
     */
    @GetMapping("/monitor/{id}")
    public Result<?> getMonitorInfo(@PathVariable Long id) {
        ServerDO server = serverService.getById(id);
        if (server == null) {
            return Result.error("服务器不存在");
        }

        try {
            return Result.success(serverService.getMonitorInfo(server));
        } catch (Exception e) {
            return Result.error("获取监控信息失败: " + e.getMessage());
        }
    }
}