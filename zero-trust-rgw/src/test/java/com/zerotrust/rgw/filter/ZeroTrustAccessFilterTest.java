package com.zerotrust.rgw.filter;

import com.zerotrust.rgw.config.GatewaySafetyProperties;
import com.zerotrust.rgw.entity.Resource;
import com.zerotrust.rgw.model.PolicyDecision;
import com.zerotrust.rgw.repository.ResourceRepository;
import com.zerotrust.rgw.service.PolicyService;
import com.zerotrust.rgw.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZeroTrustAccessFilterTest {

    private PolicyService policyService;
    private ResourceRepository resourceRepository;
    private JwtUtils jwtUtils;
    private GatewaySafetyProperties safetyProperties;
    private GatewayFilterChain chain;
    private ZeroTrustAccessFilter filter;

    @BeforeEach
    void setUp() {
        policyService = mock(PolicyService.class);
        resourceRepository = mock(ResourceRepository.class);
        jwtUtils = mock(JwtUtils.class);
        safetyProperties = new GatewaySafetyProperties();
        safetyProperties.setAllowedHosts(List.of("example.com"));
        chain = mock(GatewayFilterChain.class);
        filter = new ZeroTrustAccessFilter(policyService, resourceRepository, jwtUtils, safetyProperties);

        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void shouldRejectRequestWithoutSessionId() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/res/res-001/data")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertEquals("Missing Session ID", exchange.getResponse().getHeaders().getFirst("X-Error-Message"));
        verify(policyService, never()).getAndValidatePolicy(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectBearerRequestWhenResourceIdMismatchesToken() {
        when(policyService.isAllowedAction("GET")).thenReturn(true);
        when(jwtUtils.validateToken("access-token")).thenReturn(true);
        when(jwtUtils.getUserId("access-token")).thenReturn("client-1");
        when(jwtUtils.getResourceId("access-token")).thenReturn("res-other");
        when(jwtUtils.getClientType("access-token")).thenReturn("API_CLIENT");

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/res/res-001/data")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .header("X-Session-Id", "sess-api")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertEquals("Resource ID Mismatch", exchange.getResponse().getHeaders().getFirst("X-Error-Message"));
        verify(policyService, never()).getAndValidatePolicy(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldAcceptCookieTokenAndForwardUserRequest() {
        when(policyService.isAllowedAction("GET")).thenReturn(true);
        when(jwtUtils.validateToken("cookie-token")).thenReturn(true);
        when(jwtUtils.getUserId("cookie-token")).thenReturn("user-1");
        when(jwtUtils.getResourceId("cookie-token")).thenReturn("res-001");
        when(jwtUtils.getClientType("cookie-token")).thenReturn("USER");

        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-user");
        decision.setUserId("user-1");
        decision.setResourceId("res-001");
        decision.setAction("GET");
        decision.setAllowed(true);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("USER");

        Resource resource = new Resource();
        resource.setResourceId("res-001");
        resource.setResourceUrl("https://example.com/backend");

        when(policyService.getAndValidatePolicy(
                eq("sess-user"), eq("user-1"), eq("res-001"), eq("GET"), eq("USER"), eq(null)))
                .thenReturn(Mono.just(decision));
        when(resourceRepository.findByResourceId("res-001")).thenReturn(Mono.just(resource));

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/res/res-001/api/list")
                .cookie(new org.springframework.http.HttpCookie("access_token", "cookie-token"))
                .header("X-Session-Id", "sess-user")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(null, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getAttributes().containsKey(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR));
        verify(policyService).getAndValidatePolicy("sess-user", "user-1", "res-001", "GET", "USER", null);
        verify(resourceRepository).findByResourceId("res-001");
        verify(chain).filter(any());
    }

    @Test
    void shouldPassFingerprintForSoftwareToolRequest() {
        when(policyService.isAllowedAction("GET")).thenReturn(true);
        when(jwtUtils.validateToken("tool-token")).thenReturn(true);
        when(jwtUtils.getUserId("tool-token")).thenReturn("tool-1");
        when(jwtUtils.getResourceId("tool-token")).thenReturn("res-003");
        when(jwtUtils.getClientType("tool-token")).thenReturn("SOFTWARE_TOOL");

        PolicyDecision decision = new PolicyDecision();
        decision.setSessionId("sess-tool");
        decision.setUserId("tool-1");
        decision.setResourceId("res-003");
        decision.setAction("GET");
        decision.setAllowed(true);
        decision.setExpireTime(System.currentTimeMillis() + 60_000);
        decision.setClientType("SOFTWARE_TOOL");
        decision.setDeviceFingerprint("fp-123");

        Resource resource = new Resource();
        resource.setResourceId("res-003");
        resource.setResourceUrl("https://example.com/tool");

        when(policyService.getAndValidatePolicy(
                eq("sess-tool"), eq("tool-1"), eq("res-003"), eq("GET"), eq("SOFTWARE_TOOL"), eq("fp-123")))
                .thenReturn(Mono.just(decision));
        when(resourceRepository.findByResourceId("res-003")).thenReturn(Mono.just(resource));

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/res/res-003/download")
                .header(HttpHeaders.AUTHORIZATION, "Bearer tool-token")
                .header("X-Session-Id", "sess-tool")
                .header("X-Device-Fingerprint", "fp-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(policyService).getAndValidatePolicy("sess-tool", "tool-1", "res-003", "GET", "SOFTWARE_TOOL", "fp-123");
        verify(chain).filter(any());
    }
}
