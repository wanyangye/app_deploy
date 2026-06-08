package com.bc.app_deploy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.RoleMapper;
import com.bc.app_deploy.model.entity.RoleDO;
import com.bc.app_deploy.service.IRoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RoleDO> implements IRoleService {
}
