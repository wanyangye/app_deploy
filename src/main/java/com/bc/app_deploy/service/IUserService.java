package com.bc.app_deploy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bc.app_deploy.model.entity.UserDO;
import com.bc.app_deploy.model.vo.UserVO;

public interface IUserService extends IService<UserDO> {
    UserDO authByAccount(String account, String password);

    UserVO userInfo(Long userId);
}
