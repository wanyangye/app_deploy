package com.bc.app_deploy.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.MenuMapper;
import com.bc.app_deploy.mapper.RoleMenuMapper;
import com.bc.app_deploy.model.entity.MenuDO;
import com.bc.app_deploy.model.entity.RoleMenuDO;
import com.bc.app_deploy.service.system.IMenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, MenuDO> implements IMenuService {
    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuDO> getAllMenuTree() {
        List<MenuDO> allMenus = list(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getIsHidden, 0)
                .orderByAsc(MenuDO::getSortOrder));

        return buildMenuTree(allMenus, 0L);
    }

    private List<MenuDO> buildMenuTree(List<MenuDO> allMenus, Long parentId) {
        List<MenuDO> tree = new ArrayList<>();

        for (MenuDO menu : allMenus) {
            if (parentId.equals(menu.getParentId())) {
                List<MenuDO> children = buildMenuTree(allMenus, menu.getId());
                if (!children.isEmpty()) {
                    menu.setChildren(children);
                }
                tree.add(menu);
            }
        }

        return tree;
    }


    @Override
    public List<MenuDO> getUserMenuTree(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询角色对应的菜单ID
        List<RoleMenuDO> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenuDO>()
                        .in(RoleMenuDO::getRoleId, roleIds)
        );

        if (roleMenus.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(RoleMenuDO::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        // 查询菜单列表
        List<MenuDO> allMenus = list(new LambdaQueryWrapper<MenuDO>()
                .in(MenuDO::getId, menuIds)
                .eq(MenuDO::getIsHidden, 1)
                .eq(MenuDO::getMenuType, 1)
                .orderByAsc(MenuDO::getSortOrder));

        // 构建树形结构
        return buildMenuTree(allMenus, 0L);
    }
}
