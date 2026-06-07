package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.bc.app_deploy.model.common.BaseRelationDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_role_menu")
public class RoleMenuDO extends BaseRelationDO {

    @TableId
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;
}
