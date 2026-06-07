package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bc.app_deploy.model.common.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_server")
public class ServerDO extends BaseDO {
    private Long id;

    private String name;

    private String host;

    private Integer port;

    private String account;

    private String authType;

    private String password;

    private String privateKey;

    private String uploadPath;

    /**
     * 1-在线 0 离线
     */
    private Integer status;

    @TableLogic
    private Integer deleted;
}
