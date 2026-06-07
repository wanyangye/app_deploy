package com.bc.app_deploy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bc.app_deploy.mapper")
public class AppDeployApplication {


    public static void main(String[] args) {
        SpringApplication.run(AppDeployApplication.class, args);
    }

}
