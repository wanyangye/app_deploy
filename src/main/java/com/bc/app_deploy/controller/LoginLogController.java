package com.bc.app_deploy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bc.app_deploy.model.entity.LoginLogDO;
import com.bc.app_deploy.service.ILoginLogService;
import com.bc.app_deploy.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login-log")
public class LoginLogController {
    @Resource
    private ILoginLogService loginLogService;

    /**
     * 分页查询登录日志
     */
    @GetMapping("/list")
    public Result<Page<LoginLogDO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {

        Page<LoginLogDO> page = new Page<>(current, size);
        LambdaQueryWrapper<LoginLogDO> wrapper = new LambdaQueryWrapper<>();

        if (username != null && !username.trim().isEmpty()) {
            wrapper.like(LoginLogDO::getUsername, username);
        }

        if (status != null) {
            wrapper.eq(LoginLogDO::getStatus, status);
        }

        wrapper.orderByDesc(LoginLogDO::getLoginTime);

        return Result.success(loginLogService.page(page, wrapper));
    }

    /**
     * 删除登录日志
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        loginLogService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 清空登录日志
     */
    @DeleteMapping("/clear")
    public Result<String> clear() {
        loginLogService.remove(new LambdaQueryWrapper<>());
        return Result.success("清空成功");
    }
}
