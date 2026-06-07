package com.bc.app_deploy.framework.handler;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.bc.app_deploy.model.dto.LoginUserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class FieldAutoFillMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        long currentTimeMillis = System.currentTimeMillis();
        LocalDateTime localDateTime = LocalDateTimeUtil.of(currentTimeMillis);
        Long userId = LoginUserContext.getUserId();
        String userAccount = LoginUserContext.getUserAccount();
        this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
        this.strictInsertFill(metaObject, "createdAccount", String.class, userAccount);
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, localDateTime);
        this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
        this.strictInsertFill(metaObject, "updatedAccount", String.class, userAccount);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, localDateTime);
        this.strictInsertFill(metaObject, "lastUpdatedAt", Long.class, currentTimeMillis);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        long currentTimeMillis = System.currentTimeMillis();
        LocalDateTime localDateTime = LocalDateTimeUtil.of(currentTimeMillis);
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, LoginUserContext.getUserId());
        this.strictUpdateFill(metaObject, "updatedAccount", String.class, LoginUserContext.getUserAccount());
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, localDateTime);
        this.strictUpdateFill(metaObject, "lastUpdatedAt", Long.class, currentTimeMillis);
    }
}
