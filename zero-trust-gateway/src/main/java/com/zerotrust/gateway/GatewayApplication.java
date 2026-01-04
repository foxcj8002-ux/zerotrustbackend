package com.zerotrust.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


//注意：此网关为后端控制器的所有路由转发网关，与资源网关没关系
@SpringBootApplication()
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}