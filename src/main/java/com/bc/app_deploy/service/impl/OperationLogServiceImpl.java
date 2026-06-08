package com.bc.app_deploy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.OperationLogMapper;
import com.bc.app_deploy.model.entity.OperationLogDO;
import com.bc.app_deploy.service.IOperationLogService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLogDO> implements IOperationLogService {
}
