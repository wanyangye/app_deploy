package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_project_server")
public class ProjectServerDO {
    private Long id;

    private Long projectId;

    private Long serverId;
}
