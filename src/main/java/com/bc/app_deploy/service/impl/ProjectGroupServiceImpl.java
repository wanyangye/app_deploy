package com.bc.app_deploy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.ProjectGroupMapper;
import com.bc.app_deploy.model.entity.ProjectGroupDO;
import com.bc.app_deploy.service.IProjectGroupService;
import org.springframework.stereotype.Service;

@Service
public class ProjectGroupServiceImpl extends ServiceImpl<ProjectGroupMapper, ProjectGroupDO> implements IProjectGroupService {
}
