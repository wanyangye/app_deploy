package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.bc.app_deploy.model.common.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_menu")
public class MenuDO extends BaseDO {
    @TableId
    private Long id;

    /**
     * 父菜单ID，0表示顶级菜单
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单路径
     */
    private String path;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 路由类型：1-主菜单/目录，2-页面，3-按钮
     */
    private Integer menuType;

    /**
     * 权限标识 (按钮类型使用，如 sys:user:add)
     */
    private String permissionCode;

    /**
     * 是否隐藏: 0-否, 1-是
     */
    private Integer isHidden;

    @TableLogic
    private Integer deleted;

    /**
     * 子菜单列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<MenuDO> children;
}
