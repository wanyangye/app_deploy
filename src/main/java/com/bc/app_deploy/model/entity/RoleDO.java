package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bc.app_deploy.model.common.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_role")
public class RoleDO extends BaseDO {
    private Long id;
    private String name;
    private String description;
    private String permissionCode;
    @TableLogic
    private Integer deleted;
}
