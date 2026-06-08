package com.bc.app_deploy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.LoginLogMapper;
import com.bc.app_deploy.model.entity.LoginLogDO;
import com.bc.app_deploy.service.ILoginLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLogDO> implements ILoginLogService {
    /**
     * 异步记录登录日志
     */
    @Async
    public void recordLoginLog(LoginLogDO loginLog) {
        try {
            this.save(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }
}
