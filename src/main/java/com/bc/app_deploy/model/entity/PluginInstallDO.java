package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 插件安装记录
 */
@Data
@TableName("t_plugin_install")
public class PluginInstallDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 服务器ID
     */
    private Long serverId;
    
    /**
     * 插件ID
     */
    private Long pluginId;
    
    /**
     * 安装的版本
     */
    private String version;
    
    /**
     * 安装状态: INSTALLING / SUCCESS / FAILED / UNINSTALLING
     */
    private String status;
    
    /**
     * 安装日志
     */
    @TableField("`log`")
    private String log;
    
    /**
     * 安装路径
     */
    private String installPath;
    
    /**
     * 安装时间
     */
    private LocalDateTime installTime;
    
    /**
     * 操作人
     */
    private String operateBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
