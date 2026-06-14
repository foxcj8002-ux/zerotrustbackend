package com.zerotrust.rgw.filter;

import com.zerotrust.rgw.config.GatewaySafetyProperties;
import com.zerotrust.rgw.repository.ResourceRepository;
import com.zerotrust.rgw.service.PolicyService;
import com.zerotrust.rgw.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZeroTrustAccessFilter implements GlobalFilter, Ordered {

    private final PolicyService policyService;
    private final ResourceRepository resourceRepository;
    private final JwtUtils jwtUtils;
    private final GatewaySafetyProperties safetyProperties;

    private static final String HEADER_SESSION_ID = "X-Session-Id";
    private static final String HEADER_FINGERPRINT = "X-Device-Fingerprint";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (path.startsWith("/gateway/")) {
            return chain.filter(exchange);
        }

        if (!path.startsWith("/res/")) {
            return chain.filter(exchange);
        }

        String sessionId = request.getHeaders().getFirst(HEADER_SESSION_ID);
        String resourceId = extractResourceId(path);
        String requestAction = request.getMethod() == null ? null : request.getMethod().name();
        String requestFingerprint = request.getHeaders().getFirst(HEADER_FINGERPRINT);

        log.info("资源访问请求: resourceId={}, sessionId={}, action={}",
                resourceId, sessionId, requestAction);

        String token = resolveToken(exchange);
        if (!StringUtils.hasText(token)) {
            log.warn("缺少访问令牌");
            return unauthorized(exchange, "Missing Access Token");
        }

        if (!StringUtils.hasText(resourceId)) {
            log.warn("资源标识缺失");
            return forbidden(exchange, "Missing Resource ID");
        }

        if (!StringUtils.hasText(sessionId)) {
            log.warn("缺少会话标识");
            return forbidden(exchange, "Missing Session ID");
        }

        if (!policyService.isAllowedAction(requestAction)) {
            log.warn("请求 action 非法: resourceId={}, action={}", resourceId, requestAction);
            return forbidden(exchange, "Invalid Action");
        }

        if (!jwtUtils.validateToken(token)) {
            log.warn("访问令牌无效");
            return unauthorized(exchange, "Invalid Access Token");
        }

        String subjectId = jwtUtils.getUserId(token);
        String jwtClientType = jwtUtils.getClientType(token);

        if (!StringUtils.hasText(subjectId)) {
            log.warn("JWT 缺少主体标识");
            return unauthorized(exchange, "Missing Subject In Token");
        }

        return policyService.getAndValidatePolicy(
                        sessionId,
                        subjectId,
                        resourceId,
                        requestAction,
                        jwtClientType,
                        requestFingerprint)
                .flatMap(decision -> {
                    if (!decision.getResourceId().equals(resourceId)) {
                        return Mono.error(new AccessDeniedException(
                                "Resource ID Mismatch"));
                    }
                    return Mono.just(decision);
                })
                .flatMap(decision -> resourceRepository.findByResourceId(resourceId))
                .switchIfEmpty(Mono.error(new AccessDeniedException(
                        "Access Denied: Unauthorized or Resource not found")))
                .flatMap(resource -> forward(exchange, chain, resource.getResourceUrl(), resourceId))
                .onErrorResume(AccessDeniedException.class, e -> {
                    log.warn("访问被拒绝: resourceId={}, subjectId={}", resourceId, subjectId);
                    return forbidden(exchange, e.getMessage());
                })
                .onErrorResume(e -> {
                    log.error("网关处理异常: resourceId={}, error={}", resourceId, e.getMessage(), e);
                    return serverError(exchange, "Internal Gateway Error");
                })
                .then();
    }

    private String resolveToken(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("access_token");
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String bearerToken = authHeader.substring(7).trim();
            return StringUtils.hasText(bearerToken) ? bearerToken : null;
        }
        return null;
    }

    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }

    private Mono<Void> forward(ServerWebExchange exchange,
                               GatewayFilterChain chain,
                               String targetUrl,
                               String resourceId) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            URI targetBaseUri = URI.create(targetUrl);

            String scheme = targetBaseUri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                log.error("SSRF拦截: 不允许的协议 scheme={}, 非法目标主机名: {}",
                        scheme, targetBaseUri.getHost());
                return forbidden(exchange, "Access Denied");
            }

            String host = targetBaseUri.getHost();
            if (host == null || host.isBlank()) {
                log.error("SSRF拦截: 缺少目标主机, 请求路径: {}", request.getPath().value());
                return forbidden(exchange, "Access Denied");
            }

            if (!isHostAllowed(host)) {
                log.error("SSRF拦截: 目标主机属于黑名单或不在白名单内, 非法目标主机名: {}", host);
                return forbidden(exchange, "Access Denied");
            }

            String originalPath = request.getPath().value();
            String prefix = "/res/" + resourceId;
            String subPath = originalPath.startsWith(prefix)
                    ? originalPath.substring(prefix.length())
                    : "";
            if (subPath.isEmpty()) subPath = "/";

            String targetBasePath = (targetBaseUri.getPath() == null) ? "" : targetBaseUri.getPath();
            String finalPath = (targetBasePath + subPath).replaceAll("//+", "/");

            URI finalUri = UriComponentsBuilder.fromUri(targetBaseUri)
                    .replacePath(finalPath)
                    .queryParams(request.getQueryParams())
                    .build(true)
                    .toUri();

            ServerHttpRequest newRequest = request.mutate()
                    .method(request.getMethod())
                    .build();

            ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
            newExchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, finalUri);

            log.info("透明转发 [{}]: {} -> {}", request.getMethod(), originalPath, finalUri);
            return chain.filter(newExchange);

        } catch (Exception e) {
            log.error("转发逻辑异常: ", e);
            return serverError(exchange, "Internal Forwarding Error");
        }
    }

    private boolean isHostAllowed(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            byte[] addrBytes = address.getAddress();

            if (address.isLoopbackAddress()) return false;
            if (addrBytes.length == 4 && addrBytes[0] == (byte) 169 && addrBytes[1] == (byte) 254) return false;
            if (addrBytes.length == 4) {
                if (addrBytes[0] == (byte) 10) return false;
                if (addrBytes[0] == (byte) 172 && (addrBytes[1] & 0xf0) == (byte) 0x10) return false;
                if (addrBytes[0] == (byte) 192 && addrBytes[1] == (byte) 168) return false;
            }

            List<String> allowedHosts = safetyProperties.getAllowedHosts();
            if (allowedHosts != null && !allowedHosts.isEmpty()) {
                return allowedHosts.contains(host.toLowerCase());
            }
            return false;
        } catch (Exception e) {
            log.warn("解析目标主机失败: {}", host, e);
            return false;
        }
    }

    private String extractResourceId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 3 ? parts[2] : "";
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Error-Message", message);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("X-Error-Message", message);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> serverError(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().add("X-Error-Message", message);
        return exchange.getResponse().setComplete();
    }

    private static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
