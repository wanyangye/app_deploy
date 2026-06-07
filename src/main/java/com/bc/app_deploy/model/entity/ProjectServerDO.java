package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bc.app_deploy.model.common.BaseRelationDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_project_server")
public class ProjectServerDO extends BaseRelationDO {
    private Long id;

    private Long projectId;

    private Long serverId;
}
