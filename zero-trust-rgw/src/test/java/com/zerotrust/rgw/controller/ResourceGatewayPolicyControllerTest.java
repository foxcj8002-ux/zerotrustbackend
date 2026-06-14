package com.zerotrust.rgw.controller;

import com.zerotrust.rgw.model.PolicyDecision;
import com.zerotrust.rgw.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceGatewayPolicyControllerTest {

    private PolicyService policyService;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        policyService = mock(PolicyService.class);
        ResourceGatewayPolicyController controller = new ResourceGatewayPolicyController(policyService);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void shouldAcceptDeniedPolicyForRevocation() {
        when(policyService.isAllowedAction("DELETE")).thenReturn(true);
        when(policyService.savePolicy(any(PolicyDecision.class))).thenReturn(Mono.just(true));

        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-revoke");
        decision.setUserId("user-1");
        decision.setResourceId("res-001");
        decision.setAction("DELETE");
        decision.setAllowed(false);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("USER");

        webTestClient.post()
                .uri("/gateway/policy/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(decision)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.allowed").isEqualTo(false)
                .jsonPath("$.sessionId").isEqualTo("sess-revoke");
    }

    @Test
    void shouldRejectInvalidActionOnReceive() {
        when(policyService.isAllowedAction("READ")).thenReturn(false);

        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-invalid-action");
        decision.setUserId("user-1");
        decision.setResourceId("res-001");
        decision.setAction("READ");
        decision.setAllowed(true);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("USER");

        webTestClient.post()
                .uri("/gateway/policy/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(decision)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("action 非法，仅支持: GET / POST / PUT / DELETE / PATCH");
    }

    @Test
    void shouldRequireFingerprintForSoftwareToolPolicy() {
        when(policyService.isAllowedAction("GET")).thenReturn(true);

        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-tool");
        decision.setUserId("tool-1");
        decision.setResourceId("res-003");
        decision.setAction("GET");
        decision.setAllowed(true);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("SOFTWARE_TOOL");

        webTestClient.post()
                .uri("/gateway/policy/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(decision)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("SOFTWARE_TOOL 类型必须提供 deviceFingerprint");
    }
}
