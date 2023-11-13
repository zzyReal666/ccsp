package com.spms;

import com.ccsp.common.security.annotation.EnableRyFeignClients;
import com.ccsp.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRyFeignClients
@EnableCustomSwagger2
@SpringBootApplication
public class SpmsDbhsmApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpmsDbhsmApplication.class, args);
        System.out.println("****************");
        System.out.println("DBENC 模块启动成功");
        System.out.println("****************");
    }
}


