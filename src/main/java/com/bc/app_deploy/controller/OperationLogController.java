package com.bc.app_deploy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bc.app_deploy.model.entity.OperationLogDO;
import com.bc.app_deploy.service.IOperationLogService;
import com.bc.app_deploy.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation-log")
public class OperationLogController {
    @Resource
    private IOperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/list")
    public Result<Page<OperationLogDO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer status) {

        Page<OperationLogDO> page = new Page<>(current, size);
        LambdaQueryWrapper<OperationLogDO> wrapper = new LambdaQueryWrapper<>();

        if (username != null && !username.trim().isEmpty()) {
            wrapper.like(OperationLogDO::getUsername, username);
        }

        if (module != null && !module.trim().isEmpty()) {
            wrapper.like(OperationLogDO::getModule, module);
        }

        if (status != null) {
            wrapper.eq(OperationLogDO::getStatus, status);
        }

        wrapper.orderByDesc(OperationLogDO::getOperationTime);

        return Result.success(operationLogService.page(page, wrapper));
    }

    /**
     * 删除操作日志
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        operationLogService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 清空操作日志
     */
    @DeleteMapping("/clear")
    public Result<String> clear() {
        operationLogService.remove(new LambdaQueryWrapper<>());
        return Result.success("清空成功");
    }
}

