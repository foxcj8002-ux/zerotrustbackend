package com.zerotrust.rgw.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.rgw.enums.ClientType;
import com.zerotrust.rgw.model.PolicyDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private static final String POLICY_KEY_PREFIX = "gateway:policy:";
    private static final Set<String> ALLOWED_ACTIONS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH");

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 存储策略到 Redis，TTL 按 expireTime 计算，不再固定 30 分钟。
     */
    public Mono<Boolean> savePolicy(PolicyDecision decision) {
        if (decision == null || !StringUtils.hasText(decision.getSessionId())) {
            return Mono.just(false);
        }

        String key = POLICY_KEY_PREFIX + decision.getSessionId();
        try {
            String json = objectMapper.writeValueAsString(decision);

            long ttlMillis = decision.getExpireTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) {
                log.warn("策略已过期，拒绝写入: sessionId={}, expireTime={}",
                        decision.getSessionId(), decision.getExpireTime());
                return Mono.just(false);
            }

            Duration ttl = Duration.ofMillis(ttlMillis);
            return redisTemplate.opsForValue()
                    .set(key, json, ttl)
                    .doOnSuccess(v -> log.info("策略已存储: sessionId={}, allowed={}, ttl={}s",
                            decision.getSessionId(), decision.isAllowed(), ttl.getSeconds()))
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
     * 从 Redis 获取并验证策略。
     *
     * @param sessionId          授权票据 ID
     * @param subjectId          JWT 中的主体标识（必须存在）
     * @param resourceId         请求路径中的资源标识（必须存在）
     * @param requestAction      请求的 HTTP Method（GET/POST/...），用于与策略比对
     * @param jwtClientType      JWT 中的 clientType（可选，用于一致性校验，可为 null）
     * @param requestFingerprint 请求头中的 X-Device-Fingerprint（仅 SOFTWARE_TOOL 需要）
     */
    public Mono<PolicyDecision> getAndValidatePolicy(
            String sessionId,
            String subjectId,
            String resourceId,
            String requestAction,
            String jwtClientType,
            String requestFingerprint) {

        if (!StringUtils.hasText(sessionId)
                || !StringUtils.hasText(subjectId)
                || !StringUtils.hasText(resourceId)
                || !StringUtils.hasText(requestAction)) {
            return Mono.empty();
        }

        String normalizedAction = normalizeAction(requestAction);
        if (normalizedAction == null) {
            log.warn("请求 action 非法: sessionId={}, action={}", sessionId, requestAction);
            return Mono.empty();
        }

        String key = POLICY_KEY_PREFIX + sessionId;
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        PolicyDecision decision = objectMapper.readValue(json, PolicyDecision.class);

                        if (!StringUtils.hasText(decision.getUserId())) {
                            log.warn("策略缺少主体标识: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        if (!StringUtils.hasText(decision.getResourceId())) {
                            log.warn("策略缺少资源标识: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        if (!StringUtils.hasText(decision.getAction())) {
                            log.warn("策略缺少 action: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        if (!StringUtils.hasText(decision.getClientType())) {
                            log.warn("策略缺少 clientType: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        if (!ClientType.isValid(decision.getClientType())) {
                            log.warn("策略 clientType 非法: sessionId={}, clientType={}",
                                    sessionId, decision.getClientType());
                            return Mono.empty();
                        }
                        if (decision.getExpireTime() == null) {
                            log.warn("策略缺少 expireTime: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        if (System.currentTimeMillis() > decision.getExpireTime()) {
                            log.warn("策略已过期: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        if (!decision.isAllowed()) {
                            log.warn("策略显式拒绝访问: sessionId={}", sessionId);
                            return Mono.empty();
                        }
                        if (!decision.getUserId().equals(subjectId)) {
                            log.warn("主体标识不匹配: expected={}, actual={}", decision.getUserId(), subjectId);
                            return Mono.empty();
                        }
                        if (!decision.getResourceId().equals(resourceId)) {
                            log.warn("资源ID不匹配: expected={}, actual={}", decision.getResourceId(), resourceId);
                            return Mono.empty();
                        }

                        String policyAction = normalizeAction(decision.getAction());
                        if (policyAction == null) {
                            log.warn("策略 action 非法: sessionId={}, action={}", sessionId, decision.getAction());
                            return Mono.empty();
                        }
                        if (!policyAction.equals(normalizedAction)) {
                            log.warn("Action 不匹配: expected={}, actual={}", policyAction, normalizedAction);
                            return Mono.empty();
                        }

                        if (StringUtils.hasText(jwtClientType)
                                && !jwtClientType.equals(decision.getClientType())) {
                            log.warn("ClientType 不一致: JWT={}, PolicyDecision={}",
                                    jwtClientType, decision.getClientType());
                            return Mono.empty();
                        }

                        if (ClientType.SOFTWARE_TOOL.name().equals(decision.getClientType())) {
                            String storedFingerprint = decision.getDeviceFingerprint();
                            if (!StringUtils.hasText(storedFingerprint)) {
                                log.warn("SoftwareTool 策略缺少设备指纹: sessionId={}", sessionId);
                                return Mono.empty();
                            }
                            if (!StringUtils.hasText(requestFingerprint)) {
                                log.warn("SoftwareTool 请求缺少设备指纹: sessionId={}", sessionId);
                                return Mono.empty();
                            }
                            if (!storedFingerprint.equals(requestFingerprint)) {
                                log.warn("设备指纹不匹配: sessionId={}", sessionId);
                                return Mono.empty();
                            }
                        }

                        log.info("策略验证通过: sessionId={}, clientType={}, action={}",
                                sessionId, decision.getClientType(), policyAction);
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

    public boolean isAllowedAction(String action) {
        return normalizeAction(action) != null;
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            return null;
        }
        String normalizedAction = action.trim().toUpperCase();
        return ALLOWED_ACTIONS.contains(normalizedAction) ? normalizedAction : null;
    }
}
