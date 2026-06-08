package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.bc.app_deploy.model.common.BaseDO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_project_group")
public class ProjectGroupDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private String owner;
    private String ownerContact;

    @TableLogic
    private Integer deleted;

    // 项目数量（非数据库字段）
    @TableField(exist = false)
    private Integer projectCount;
}
