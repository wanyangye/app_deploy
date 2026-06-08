package com.bc.app_deploy.service;

public interface IBuildExecutorService {
    void executeBuild(Long id);

    void executeRollback(Long id, Long buildId);

    void executeRollbackByBackup(Long id, Long projectId, String backupFileName);
}
