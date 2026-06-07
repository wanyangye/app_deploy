package com.bc.app_deploy.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.UserRoleMapper;
import com.bc.app_deploy.model.entity.RoleDO;
import com.bc.app_deploy.model.entity.UserRoleDO;
import com.bc.app_deploy.service.user.IRoleService;
import com.bc.app_deploy.service.user.IUserRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRoleDO> implements IUserRoleService {
    @Resource
    private IRoleService roleService;

    @Override
    public List<RoleDO> getUserRoles(long userId) {
        List<UserRoleDO> userRoles = list(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getUserId, userId));

        List<Long> roleIds = userRoles.stream()
                .map(UserRoleDO::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return List.of();
        }

        return roleService.listByIds(roleIds);
    }


    /**
     * 为用户分配角色
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色
        remove(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getUserId, userId));

        // 添加新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRoleDO> userRoles = roleIds.stream()
                    .map(roleId -> {
                        UserRoleDO userRole = new UserRoleDO();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());

            saveBatch(userRoles);
        }
    }

}
