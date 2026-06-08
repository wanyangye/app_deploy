package com.bc.app_deploy.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.bc.app_deploy.model.dto.LoginResponse;
import com.bc.app_deploy.model.entity.MenuDO;
import com.bc.app_deploy.model.entity.RoleDO;
import com.bc.app_deploy.model.entity.UserDO;
import com.bc.app_deploy.model.param.LoginParam;
import com.bc.app_deploy.model.vo.UserVO;
import com.bc.app_deploy.service.IMenuService;
import com.bc.app_deploy.service.IUserRoleService;
import com.bc.app_deploy.service.IUserService;
import com.bc.app_deploy.utils.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/auth")
public class AuthController {
    @Resource
    private IUserService userService;
    @Resource
    private IMenuService menuService;
    @Resource
    private IUserRoleService userRoleService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginParam param, HttpServletRequest request) {
        UserDO userDO = userService.authByAccount(param.getAccount(), param.getPassword());

        StpUtil.login(userDO.getId());

        LoginResponse response = new LoginResponse();
        response.setTokenName(StpUtil.getTokenName());
        response.setTokenValue(StpUtil.getTokenValue());
        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        StpUtil.logout();
        return Result.success("登出");
    }

    @GetMapping("/user-info")
    public Result<UserVO> userInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserVO user = userService.userInfo(userId);
        return Result.success(user);
    }

    @GetMapping("permission-tree")
    public Result<List<MenuDO>> permissionTree() {
        List<MenuDO> menuTree = menuService.getAllMenuTree();
        return Result.success(menuTree);
    }

    @GetMapping("permission")
    public Result<List<MenuDO>> permission() {
        String userId = StpUtil.getLoginIdAsString();
        List<RoleDO> roles = userRoleService.getUserRoles(Long.parseLong(userId));

        List<Long> roleIds = roles.stream()
                .map(RoleDO::getId)
                .collect(Collectors.toList());

        List<MenuDO> menuTree = menuService.getUserMenuTree(roleIds);
        return Result.success(menuTree);
    }
}