package com.bc.app_deploy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.ProjectMemberMapper;
import com.bc.app_deploy.model.entity.ProjectMemberDO;
import com.bc.app_deploy.model.entity.UserDO;
import com.bc.app_deploy.service.IProjectMemberService;
import com.bc.app_deploy.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectMemberServiceImpl extends ServiceImpl<ProjectMemberMapper, ProjectMemberDO> implements IProjectMemberService {
    @Resource
    private IUserService userService;

    /**
     * 获取用户有权限的项目ID列表
     */
    public List<Long> getUserProjectIds(Long userId) {
        List<ProjectMemberDO> members = list(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getUserId, userId));

        return members.stream()
                .map(ProjectMemberDO::getProjectId)
                .collect(Collectors.toList());
    }

    /**
     * 添加项目成员
     */
    public void addMember(Long projectId, Long userId, String roleType) {
        ProjectMemberDO member = new ProjectMemberDO();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRoleType(roleType);
        save(member);
    }

    /**
     * 检查用户是否有项目权限
     */
    public boolean hasProjectPermission(Long userId, Long projectId) {
        return count(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getUserId, userId)
                .eq(ProjectMemberDO::getProjectId, projectId)) > 0;
    }

    /**
     * 检查用户对项目的权限类型
     */
    public String getProjectRole(Long userId, Long projectId) {
        ProjectMemberDO member = getOne(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getUserId, userId)
                .eq(ProjectMemberDO::getProjectId, projectId));

        return member != null ? member.getRoleType() : null;
    }

    /**
     * 移除项目成员
     */
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        remove(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getProjectId, projectId)
                .eq(ProjectMemberDO::getUserId, userId));
    }

    /**
     * 获取项目的所有成员（包含用户信息）
     */
    public List<ProjectMemberDO> getProjectMembers(Long projectId) {
        List<ProjectMemberDO> members = list(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getProjectId, projectId));

        // 填充用户信息
        members.forEach(member -> {
            UserDO user = userService.getById(member.getUserId());
            if (user != null) {
                user.setPassword(null);
                member.setUser(user);
            }
        });

        return members;
    }

    /**
     * 批量分配项目成员
     */
    @Transactional
    public void batchAssignMembers(Long projectId, List<ProjectMemberDO> members) {
        // 先删除非拥有者的成员（保留拥有者）
        remove(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getProjectId, projectId)
                .ne(ProjectMemberDO::getRoleType, "OWNER"));

        // 添加新成员
        if (members != null && !members.isEmpty()) {
            members.forEach(member -> {
                member.setProjectId(projectId);
                // 检查是否已存在（避免重复）
                long count = count(new LambdaQueryWrapper<ProjectMemberDO>()
                        .eq(ProjectMemberDO::getProjectId, projectId)
                        .eq(ProjectMemberDO::getUserId, member.getUserId()));

                if (count == 0) {
                    save(member);
                }
            });
        }
    }
}
