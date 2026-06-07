package com.bc.app_deploy.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bc.app_deploy.model.common.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_user")
public class UserDO extends BaseDO {
    /**
     * ID
     */
    @TableId
    private Long id;
    /**
     * 账号
     */
    private String account;
    /**
     * 密码
     */
    private String password;
    /**
     * 姓名
     */
    private String fullName;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 状态 0:禁用 1:启用
     */
    private Integer status;
    /**
     * 删除标志 0:未删除 1:已删除
     */
    @TableLogic
    private Integer deleted;


    @TableField(exist = false)
    private java.util.List<RoleDO> roles;
}
