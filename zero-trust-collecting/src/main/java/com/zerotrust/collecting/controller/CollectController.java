package com.zerotrust.collecting.controller;

import com.zerotrust.collecting.collector.EnvironmentCollector;
import com.zerotrust.collecting.common.entity.SecurityContextSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/collect")
public class CollectController {
    @Autowired
    private EnvironmentCollector environmentCollector;

    @PostMapping()
    public SecurityContextSnapshot collect(HttpServletRequest request) {
        LocalDateTime startTime = LocalDateTime.now();

        SecurityContextSnapshot snapshot = SecurityContextSnapshot.builder()
                .requestId(UUID.randomUUID().toString())
                .userId("demo-user")//从casdoor里面读取
                .build();
        snapshot.setCollectedAt(startTime);
        // 这里调用所有维度的 collector
        environmentCollector.collect(snapshot, request);
        // networkCollector.collect(snapshot, request);
        // sessionCollector.collect(snapshot, request);
        // ...
//        // 发到 MQ，给 monitoring 用
//        rabbitTemplate.convertAndSend("zt.collect.exchange",
//                "collect.security.context",
//                snapshot);
        return snapshot;
    }
}
