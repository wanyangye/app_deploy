package com.bc.app_deploy.service.system.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.ProjectServerMapper;
import com.bc.app_deploy.model.entity.ProjectServerDO;
import com.bc.app_deploy.service.system.IProjectServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectServerServiceImpl extends ServiceImpl<ProjectServerMapper, ProjectServerDO> implements IProjectServerService {
}
