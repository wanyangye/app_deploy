package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.bc.app_deploy.model.common.BaseDO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_project")
public class ProjectDO extends BaseDO {
    @TableId
    private Long id;

    private String name;

    private String description;

    private String gitUrl;

    private String branch;

    private String gitAccount;

    private String gitPassword;

    private String projectType;

    private String buildCommand;

    private String buildDir;

    private String projectDir;

    private Long serverId;

    private Integer autoDeploy;

    private String deployScript;

    private String deployPath;

    private Integer appPort;

    private String env;

    private Long groupId;

    @TableLogic
    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    // 最近部署时间（非数据库字段）
    @TableField(exist = false)
    private LocalDateTime lastDeployTime;
}
