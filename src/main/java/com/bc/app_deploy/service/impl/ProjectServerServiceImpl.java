package com.bc.app_deploy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.ProjectServerMapper;
import com.bc.app_deploy.model.entity.ProjectServerDO;
import com.bc.app_deploy.service.IProjectServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ProjectServerServiceImpl extends ServiceImpl<ProjectServerMapper, ProjectServerDO> implements IProjectServerService {
    /**
     * 获取项目的服务器ID列表
     */
    public List<Long> getProjectServerIds(Long projectId) {
        LambdaQueryWrapper<ProjectServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectServerDO::getProjectId, projectId);
        return list(wrapper).stream()
                .map(ProjectServerDO::getServerId)
                .toList();
    }

    /**
     * 为项目分配服务器
     */
    @Transactional
    public void assignServers(Long projectId, List<Long> serverIds) {
        // 删除旧的关联
        LambdaQueryWrapper<ProjectServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectServerDO::getProjectId, projectId);
        remove(wrapper);

        // 添加新的关联
        if (serverIds != null && !serverIds.isEmpty()) {
            for (Long serverId : serverIds) {
                ProjectServerDO projectServer = new ProjectServerDO();
                projectServer.setProjectId(projectId);
                projectServer.setServerId(serverId);
                save(projectServer);
            }
        }
    }
}
