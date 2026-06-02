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
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.InetAddress;
import java.net.URI;
import org.springframework.http.HttpCookie;
import java.util.List;  
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.web.util.UriComponentsBuilder;
@Slf4j
@Component
@RequiredArgsConstructor
public class ZeroTrustAccessFilter implements GlobalFilter, Ordered {

    private final PolicyService policyService;
    private final ResourceRepository resourceRepository;
    private final JwtUtils jwtUtils;

    // @Value("#{'${gateway.safety.allowed-hosts}'.split(',')}")
    // private List<String> allowedHosts;
    private final GatewaySafetyProperties safetyProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 跳过内部接口
        if (path.startsWith("/gateway/")) {
            return chain.filter(exchange);
        }

        // 仅处理资源入口
        if (!path.startsWith("/res/")) {
            return chain.filter(exchange);
        }

        //String authHeader = request.getHeaders().getFirst("Authorization");
        HttpCookie tokenCookie = request.getCookies().getFirst("access_token");

        String sessionId = request.getHeaders().getFirst("X-Session-Id");
        String resourceId = extractResourceId(path);

        log.info("资源访问请求: resourceId={}, sessionId={}", resourceId, sessionId);
        // log.warn("{}",request);

        // 2. 验证 Cookie 是否存在
        if (tokenCookie == null || tokenCookie.getValue().isBlank()) {
            log.warn("缺少访问令牌(Cookie)");
            return unauthorized(exchange, "Missing Access Token");
        }

        if (resourceId.isBlank()) {
            log.warn("资源资源标识缺失");
            return forbidden(exchange, "Missing Resource ID");
        }

        // if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        //     log.warn("缺少访问令牌");
        //     return unauthorized(exchange, "缺少访问令牌");
        // }

        //String token = authHeader.substring(7);
        String token = tokenCookie.getValue();
        if (!jwtUtils.validateToken(token)) {
            log.warn("访问令牌无效");
            return unauthorized(exchange, "Invalid Access Token");
        }

        String userId = jwtUtils.getUserId(token);
        String tokenResourceId = jwtUtils.getResourceId(token);

        if (tokenResourceId != null && !tokenResourceId.equals(resourceId)) {
            log.warn("资源标识不匹配: tokenResourceId={}, requestResourceId={}", tokenResourceId, resourceId);
            return forbidden(exchange, "Resource ID Mismatch");
        }

        if (sessionId == null || sessionId.isBlank()) {
            log.warn("缺少会话标识");
            return forbidden(exchange, "Missing Session ID");
        }

        // 先处理“空结果”再进入转发
        return policyService.getAndValidatePolicy(sessionId, userId, resourceId)
                .flatMap(policy -> resourceRepository.findByResourceId(resourceId))
                .switchIfEmpty(Mono.error(new AccessDeniedException("Access Denied: Unauthorized or Resource not found")))
                // 执行完转发后，返回 Mono<Void>
                .flatMap(resource -> forward(exchange, chain, resource.getResourceUrl(), resourceId))
                .onErrorResume(AccessDeniedException.class, e -> {
                    log.warn("访问被拒绝: resourceId={}, userId={}", resourceId, userId);
                    return forbidden(exchange, e.getMessage());
                })
                .onErrorResume(e -> {
                    log.error("网关处理异常: resourceId={}, error={}", resourceId, e.getMessage(), e);
                    return serverError(exchange, "Internal Gateway Error");
                })
                .then(); // 强制转换为 Mono<Void>
    }

    /**
     * 关键：在 RouteToRequestUrlFilter 之后执行，防止目标 URI 被默认路由覆盖
     */
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

        // === 1. SSRF 检查 ===
        String scheme = targetBaseUri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            // 满足日志记录要求：包含"SSRF拦截"及目标主机名
            log.error("SSRF拦截: 不允许的协议 scheme={}, 非法目标主机名: {}", scheme, targetBaseUri.getHost());
            // 满足拓扑隐身要求：返回统一且模糊的 403 错误，不泄露验证细节
            return forbidden(exchange, "Access Denied");
        }

        String host = targetBaseUri.getHost();
        if (host == null || host.isBlank()) {
            log.error("SSRF拦截: 缺少目标主机, 请求路径: {}", request.getPath().value());
            return forbidden(exchange, "Access Denied");
        }

        if (!isHostAllowed(host)) {
            // 满足非法目标熔断及安全日志审计要求
            log.error("SSRF拦截: 目标主机属于黑名单(Loopback/Metadata)或不在白名单内, 非法目标主机名: {}", host);
            // 状态反馈一致性：网关必须返回 403 Forbidden 状态码，且不携带细化特征指纹
            return forbidden(exchange, "Access Denied");
        }

        // === 2. 计算子路径 ===
        String originalPath = request.getPath().value();
        String prefix = "/res/" + resourceId;
        String subPath = originalPath.startsWith(prefix) 
                         ? originalPath.substring(prefix.length()) 
                         : "";
        if (subPath.isEmpty()) subPath = "/";

        String targetBasePath = (targetBaseUri.getPath() == null) ? "" : targetBaseUri.getPath();
        String finalPath = (targetBasePath + subPath).replaceAll("//+", "/");

        // === 3. 构建最终 URI ===
        URI finalUri = UriComponentsBuilder.fromUri(targetBaseUri)
                .replacePath(finalPath)
                .queryParams(request.getQueryParams())
                .build(true)
                .toUri();

        // === 4. 构造新请求 ===
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

            // 黑名单 1: loopback
            if (address.isLoopbackAddress()) return false;
            // 黑名单 2: 链路本地 169.254.x.x（云元数据地址段）
            if (addrBytes.length == 4 && addrBytes[0] == (byte) 169 && addrBytes[1] == (byte) 254) return false;
            // 黑名单 3: RFC1918 私网地址
            if (addrBytes.length == 4) {
                if (addrBytes[0] == (byte) 10) return false;
                if (addrBytes[0] == (byte) 172 && (addrBytes[1] & 0xf0) == (byte) 0x10) return false;
                if (addrBytes[0] == (byte) 192 && addrBytes[1] == (byte) 168) return false;
            }
    
            // 从配置类中实时获取列表
            List<String> allowedHosts = safetyProperties.getAllowedHosts();
            // log.info("【DEBUG】当前内存中的白名单列表内容: {}", allowedHosts);
            // log.info("【DEBUG】正在比对的目标主机: {}", host);

            // 白名单
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