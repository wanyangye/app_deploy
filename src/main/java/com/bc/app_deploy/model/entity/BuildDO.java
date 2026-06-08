package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("t_build")
public class BuildDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;

    private String status;

    private String log;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private Long duration;

    private String triggerBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;

    // 项目名称（非数据库字段）
    @TableField(exist = false)
    private String projectName;

    // 环境（非数据库字段，从项目中获取）
    @TableField(exist = false)
    private String env;

    // 触发人名称（非数据库字段）
    @TableField(exist = false)
    private String triggerByName;
}
