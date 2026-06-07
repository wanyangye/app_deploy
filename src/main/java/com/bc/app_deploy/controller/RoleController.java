package com.bc.app_deploy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bc.app_deploy.model.entity.RoleDO;
import com.bc.app_deploy.service.user.IRoleService;
import com.bc.app_deploy.utils.Result;
import jakarta.annotation.Resource;
import org.apache.catalina.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role")
public class RoleController {
    @Resource
    private IRoleService roleService;

    @GetMapping("/list")
    public Result<Page<RoleDO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        Page<RoleDO> page = new Page<>(current, size);
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(RoleDO::getName, name);
        }

        wrapper.orderByAsc(RoleDO::getId);

        return Result.success(roleService.page(page, wrapper));
    }

    @GetMapping("/all")
    public Result<List<RoleDO>> all() {
        return Result.success(roleService.list());
    }

    @GetMapping("/{id}")
    public Result<RoleDO> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    public Result<String> add(@RequestBody RoleDO role) {
        roleService.save(role);
        return Result.success("添加成功");
    }


    @PutMapping
    public Result<String> update(@RequestBody RoleDO role) {
        roleService.updateById(role);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success("删除成功");
    }
}
