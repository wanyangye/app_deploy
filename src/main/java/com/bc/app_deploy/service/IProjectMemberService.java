package com.bc.app_deploy.service;

import com.bc.app_deploy.model.entity.ProjectMemberDO;

import java.util.List;

public interface IProjectMemberService {
    List<Long> getUserProjectIds(Long userId);

    void addMember(Long id, Long userId, String owner);

    String getProjectRole(Long userId, Long projectId);

    List<ProjectMemberDO> getProjectMembers(Long projectId);

    void batchAssignMembers(Long projectId, List<ProjectMemberDO> members);

    void removeMember(Long projectId, Long memberId);
}
