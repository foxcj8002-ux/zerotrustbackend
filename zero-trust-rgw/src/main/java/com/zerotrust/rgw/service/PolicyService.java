package com.zerotrust.rgw.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.rgw.model.PolicyDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {
    
    private static final String POLICY_KEY_PREFIX = "gateway:policy:";
    private static final Duration POLICY_TTL = Duration.ofMinutes(30);
    
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 存储策略到 Redis
     */
    public Mono<Boolean> savePolicy(PolicyDecision decision) {
        if (decision == null || decision.getSessionId() == null) {
            return Mono.just(false);
        }
        
        String key = POLICY_KEY_PREFIX + decision.getSessionId();
        try {
            String json = objectMapper.writeValueAsString(decision);
            return redisTemplate.opsForValue()
                    .set(key, json, POLICY_TTL)
                    .doOnSuccess(v -> log.info("策略已存储: sessionId={}", decision.getSessionId()))
                    .onErrorResume(e -> {
                        log.error("存储策略失败", e);
                        return Mono.just(false);
                    });
        } catch (JsonProcessingException e) {
            log.error("策略序列化失败", e);
            return Mono.just(false);
        }
    }
    
    /**
     * 从 Redis 获取并验证策略
     */
    public Mono<PolicyDecision> getAndValidatePolicy(String sessionId, String userId, String resourceId) {
        if (sessionId == null) {
            return Mono.empty();
        }
        
        String key = POLICY_KEY_PREFIX + sessionId;
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        PolicyDecision decision = objectMapper.readValue(json, PolicyDecision.class);
                        
                        // 验证
                        if (!decision.isAllowed()) {
                            log.warn("策略不允许访问: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        if (userId != null && !decision.getUserId().equals(userId)) {
                            log.warn("用户ID不匹配: expected={}, actual={}", decision.getUserId(), userId);
                            return Mono.empty();
                        }
                        if (resourceId != null && !decision.getResourceId().equals(resourceId)) {
                            log.warn("资源ID不匹配: expected={}, actual={}", decision.getResourceId(), resourceId);
                            return Mono.empty();
                        }
                        if (decision.getExpireTime() != null && 
                            System.currentTimeMillis() > decision.getExpireTime()) {
                            log.warn("策略已过期: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        
                        log.info("策略验证通过: sessionId={}", sessionId);
                        return Mono.just(decision);
                    } catch (JsonProcessingException e) {
                        log.error("策略反序列化失败", e);
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("未找到策略: sessionId={}", sessionId);
                    return Mono.empty();
                }));
    }
}