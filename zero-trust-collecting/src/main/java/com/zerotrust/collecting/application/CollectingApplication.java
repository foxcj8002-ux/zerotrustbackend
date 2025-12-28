package com.zerotrust.collecting.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(value= "com.zerotrust.collecting.feign")
@ComponentScan(basePackages = "com.zerotrust")
public class CollectingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollectingApplication.class, args);
    }
}