package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.bc.app_deploy.model.common.BaseDO;
import com.bc.app_deploy.model.common.BaseRelationDO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_project_member")
public class ProjectMemberDO extends BaseRelationDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;

    private Long userId;

    /**
     * 角色类型：OWNER-拥有者，DEVELOPER-开发者，MEMBER-成员
     */
    private String roleType;

    /**
     * 用户信息（非数据库字段）
     */
    @TableField(exist = false)
    private UserDO user;
}
