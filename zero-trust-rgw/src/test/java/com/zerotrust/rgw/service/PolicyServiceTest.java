package com.zerotrust.rgw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.rgw.model.PolicyDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyServiceTest {

    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOperations;
    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        policyService = new PolicyService(redisTemplate, new ObjectMapper());
    }

    @Test
    void shouldValidateUserPolicy() throws Exception {
        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-user");
        decision.setUserId("user-1");
        decision.setResourceId("res-001");
        decision.setAction("GET");
        decision.setAllowed(true);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("USER");

        String json = new ObjectMapper().writeValueAsString(decision);
        when(valueOperations.get("gateway:policy:sess-user")).thenReturn(Mono.just(json));

        StepVerifier.create(policyService.getAndValidatePolicy(
                        "sess-user",
                        "user-1",
                        "res-001",
                        "GET",
                        null,
                        null))
                .expectNextMatches(policy -> "USER".equals(policy.getClientType()))
                .verifyComplete();
    }

    @Test
    void shouldRejectExplicitDeniedPolicy() throws Exception {
        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-denied");
        decision.setUserId("client-1");
        decision.setResourceId("res-002");
        decision.setAction("POST");
        decision.setAllowed(false);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("API_CLIENT");

        String json = new ObjectMapper().writeValueAsString(decision);
        when(valueOperations.get("gateway:policy:sess-denied")).thenReturn(Mono.just(json));

        StepVerifier.create(policyService.getAndValidatePolicy(
                        "sess-denied",
                        "client-1",
                        "res-002",
                        "POST",
                        "API_CLIENT",
                        null))
                .verifyComplete();
    }

    @Test
    void shouldRequireFingerprintForSoftwareTool() throws Exception {
        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-tool");
        decision.setUserId("tool-1");
        decision.setResourceId("res-003");
        decision.setAction("GET");
        decision.setAllowed(true);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("SOFTWARE_TOOL");
        decision.setDeviceFingerprint("fp-123");

        String json = new ObjectMapper().writeValueAsString(decision);
        when(valueOperations.get("gateway:policy:sess-tool")).thenReturn(Mono.just(json));

        StepVerifier.create(policyService.getAndValidatePolicy(
                        "sess-tool",
                        "tool-1",
                        "res-003",
                        "GET",
                        "SOFTWARE_TOOL",
                        null))
                .verifyComplete();

        StepVerifier.create(policyService.getAndValidatePolicy(
                        "sess-tool",
                        "tool-1",
                        "res-003",
                        "GET",
                        "SOFTWARE_TOOL",
                        "fp-123"))
                .expectNextMatches(policy -> "fp-123".equals(policy.getDeviceFingerprint()))
                .verifyComplete();
    }

    @Test
    void shouldRejectInvalidAction() throws Exception {
        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-action");
        decision.setUserId("user-2");
        decision.setResourceId("res-004");
        decision.setAction("READ");
        decision.setAllowed(true);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("USER");

        String json = new ObjectMapper().writeValueAsString(decision);
        when(valueOperations.get("gateway:policy:sess-action")).thenReturn(Mono.just(json));

        StepVerifier.create(policyService.getAndValidatePolicy(
                        "sess-action",
                        "user-2",
                        "res-004",
                        "GET",
                        "USER",
                        null))
                .verifyComplete();
    }

    @Test
    void shouldSaveDeniedPolicyWithTtlAlignment() {
        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-save");
        decision.setUserId("user-3");
        decision.setResourceId("res-005");
        decision.setAction("DELETE");
        decision.setAllowed(false);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("USER");

        when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(policyService.savePolicy(decision))
                .expectNext(true)
                .verifyComplete();
    }
}
