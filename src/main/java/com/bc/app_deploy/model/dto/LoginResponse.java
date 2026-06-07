package com.bc.app_deploy.model.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String tokenName;
    private String tokenValue;
}
