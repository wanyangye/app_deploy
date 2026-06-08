package com.bc.app_deploy.service;

import com.bc.app_deploy.model.entity.RoleDO;

import java.util.List;

public interface IUserRoleService {
    List<RoleDO> getUserRoles(long userId);

    void assignRoles(Long id, List<Long> roleIds);

    boolean hasRole(Long userId, String admin);
}
