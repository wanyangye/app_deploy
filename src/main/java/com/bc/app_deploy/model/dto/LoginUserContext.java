package com.bc.app_deploy.model.dto;

import com.bc.app_deploy.exception.BaseException;
import com.bc.app_deploy.model.constant.CommonConstant;
import com.bc.app_deploy.model.enums.LocaleEnum;

import java.util.Locale;

public class LoginUserContext {
    private static final ThreadLocal<LoginUserDTO> LOGIN_USER_DTO_THREAD_LOCAL = new ThreadLocal<>();

    public static LoginUserDTO getLoginUser() {
        LoginUserDTO loginUser = LOGIN_USER_DTO_THREAD_LOCAL.get();
        if (loginUser == null) {
            throw new BaseException("登录状态已失效，请重新登录");
        }
        return loginUser;
    }

    public static LoginUserDTO getLoginUser(boolean throwEx) {
        LoginUserDTO loginUser = LOGIN_USER_DTO_THREAD_LOCAL.get();
        if (throwEx && loginUser == null) {
            throw new BaseException("登录状态已失效，请重新登录");
        }
        return loginUser;
    }

    public static void setLoginUser(LoginUserDTO loginUserDTO) {
        LOGIN_USER_DTO_THREAD_LOCAL.set(loginUserDTO);
    }

    public static Long getUserId() {
        LoginUserDTO loginUser = LOGIN_USER_DTO_THREAD_LOCAL.get();
        if (loginUser == null || loginUser.getId() == null) {
            return CommonConstant.SYS_ADMIN_ID;
        }
        return loginUser.getId();
    }

    public static Long getLoginUserId() {
        LoginUserDTO loginUser = getLoginUser();
        return loginUser.getId();
    }

    public static String getLoginUserAccount() {
        LoginUserDTO loginUser = getLoginUser();
        return loginUser.getAccount();
    }

    public static String getUserAccount() {
        LoginUserDTO loginUser = LOGIN_USER_DTO_THREAD_LOCAL.get();
        if (loginUser == null || loginUser.getAccount() == null) {
            return "admin";
        }
        return loginUser.getAccount();
    }

    public static Locale getUserLocale(boolean returnDefault) {
        LoginUserDTO loginUser = LOGIN_USER_DTO_THREAD_LOCAL.get();
        if (loginUser != null) {
            return LocaleEnum.ofLocale(loginUser.getLanguage());
        }
        return returnDefault ? Locale.SIMPLIFIED_CHINESE : null;
    }

    public static void clear() {
        LOGIN_USER_DTO_THREAD_LOCAL.remove();
    }
}
