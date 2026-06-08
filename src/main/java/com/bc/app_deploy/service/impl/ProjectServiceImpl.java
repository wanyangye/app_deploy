package com.bc.app_deploy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.ProjectMapper;
import com.bc.app_deploy.model.entity.ProjectDO;
import com.bc.app_deploy.service.IProjectService;
import org.springframework.stereotype.Service;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, ProjectDO> implements IProjectService {
}
