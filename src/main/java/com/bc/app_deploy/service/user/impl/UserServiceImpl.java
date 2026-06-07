package com.bc.app_deploy.service.user.impl;

import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.exception.BaseException;
import com.bc.app_deploy.mapper.UserMapper;
import com.bc.app_deploy.model.entity.UserDO;
import com.bc.app_deploy.model.vo.UserVO;
import com.bc.app_deploy.service.user.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements IUserService {
    @Override
    public UserDO authByAccount(String account, String password) {
        String encryptedPassword = MD5.create().digestHex(password);
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getAccount, account)
                .eq(UserDO::getPassword, encryptedPassword);
        List<UserDO> list = list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            UserDO userDO = list.get(0);
            Integer status = userDO.getStatus();
            if (status != null && status == 1) {
                return userDO;
            }
            log.error("账号:{}已禁用", account);
            throw new BaseException(401, "账号已禁用");
        }
        log.error("账号:{}或密码错误", account);
        throw new BaseException(401, "账号或密码错误");
    }

    @Override
    public UserVO userInfo(Long userId) {
        UserVO userVO = new UserVO();
        userVO.setId(userId);
        UserDO byId = getById(userId);
        BeanUtils.copyProperties(byId, userVO);
        return userVO;
    }
}
