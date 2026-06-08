package com.bc.app_deploy.service;

import com.bc.app_deploy.model.entity.MenuDO;

import java.util.List;

public interface IMenuService {
    List<MenuDO> getAllMenuTree();

    List<MenuDO> getUserMenuTree(List<Long> roleIds);
}