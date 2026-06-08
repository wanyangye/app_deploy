package com.bc.app_deploy.service;

import com.baomidou.mybatisplus.extension.service.IService;

public interface IPluginExecutorService {
    void executeUninstall(Long installId);

    void executeInstall(Long id);
}
