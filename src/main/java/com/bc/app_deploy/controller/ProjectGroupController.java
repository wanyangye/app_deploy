package com.bc.app_deploy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bc.app_deploy.model.entity.ProjectDO;
import com.bc.app_deploy.model.entity.ProjectGroupDO;
import com.bc.app_deploy.service.IProjectGroupService;
import com.bc.app_deploy.service.IProjectService;
import com.bc.app_deploy.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project-group")
public class ProjectGroupController {
    @Resource
    private IProjectGroupService projectGroupService;

    @Resource
    private IProjectService projectService;

    @GetMapping("/list")
    public Result<Page<ProjectGroupDO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        Page<ProjectGroupDO> page = new Page<>(current, size);
        LambdaQueryWrapper<ProjectGroupDO> wrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            wrapper.like(ProjectGroupDO::getName, name);
        }

        wrapper.orderByDesc(ProjectGroupDO::getCreatedAt);

        Page<ProjectGroupDO> groupPage = projectGroupService.page(page, wrapper);

        // 填充项目数量
        groupPage.getRecords().forEach(group -> {
            LambdaQueryWrapper<ProjectDO> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(ProjectDO::getGroupId, group.getId());
            long count = projectService.count(projectWrapper);
            group.setProjectCount((int) count);
        });

        return Result.success(groupPage);
    }

    @GetMapping("/{id}")
    public Result<ProjectGroupDO> getById(@PathVariable Long id) {
        ProjectGroupDO group = projectGroupService.getById(id);
        return Result.success(group);
    }


    @PostMapping
    public Result<String> save(@RequestBody ProjectGroupDO projectGroup) {
        // 检查项目组名称是否重复
        LambdaQueryWrapper<ProjectGroupDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectGroupDO::getName, projectGroup.getName());
        if (projectGroupService.count(wrapper) > 0) {
            return Result.error("项目组名称已存在");
        }

        projectGroupService.save(projectGroup);
        return Result.success("添加成功");
    }

    @PutMapping
    public Result<String> update(@RequestBody ProjectGroupDO projectGroup) {
        // 检查项目组名称是否重复（排除自己）
        LambdaQueryWrapper<ProjectGroupDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectGroupDO::getName, projectGroup.getName())
                .ne(ProjectGroupDO::getId, projectGroup.getId());
        if (projectGroupService.count(wrapper) > 0) {
            return Result.error("项目组名称已存在");
        }

        projectGroupService.updateById(projectGroup);
        return Result.success("更新成功");
    }


    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        // 检查项目组下是否有项目
        LambdaQueryWrapper<ProjectDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectDO::getGroupId, id);
        long count = projectService.count(wrapper);
        if (count > 0) {
            return Result.error("该项目组下还有项目，无法删除");
        }

        projectGroupService.removeById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/all")
    public Result<java.util.List<ProjectGroupDO>> all() {
        LambdaQueryWrapper<ProjectGroupDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ProjectGroupDO::getCreatedAt);
        return Result.success(projectGroupService.list(wrapper));
    }
}