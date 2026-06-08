package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 插件信息实体
 */
@Data
@TableName("t_plugin")
public class PluginDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 插件名称
     */
    private String name;

    /**
     * 插件标识(唯一)
     */
    private String pluginKey;

    /**
     * 插件分类: BASE_ENV(基础环境) / MIDDLEWARE(中间件)
     */
    private String category;

    /**
     * 插件描述
     */
    private String description;

    /**
     * 插件图标
     */
    private String icon;

    /**
     * 支持的版本列表(JSON格式)
     */
    private String versions;

    /**
     * 默认版本
     */
    private String defaultVersion;

    /**
     * 安装脚本模板
     */
    @TableField("`install_script`")
    private String installScript;

    /**
     * 卸载脚本模板
     */
    private String uninstallScript;

    /**
     * 检测脚本(检查是否已安装)
     */
    private String checkScript;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态: 1- 启用 - 0- 禁用
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
