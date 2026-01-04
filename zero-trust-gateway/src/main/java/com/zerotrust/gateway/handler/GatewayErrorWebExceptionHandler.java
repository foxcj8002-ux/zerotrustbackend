package com.zerotrust.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import zerotrust.common.Response.ErrorCode;
import zerotrust.common.Response.Result;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Gateway 全局异常处理（WebFlux）
 *
 * 目标：
 * - 网关自身异常（无路由、下游不可用、超时、限流等）统一返回 Result.fail(...)
 * - 只兜底“网关处理链路中抛出的异常”，不负责改写下游正常返回的 body
 */

@Order(-2)
@Component
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 响应已提交则交给默认流程
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        Mapped mapped = mapException(ex);

        exchange.getResponse().setStatusCode(mapped.httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bodyBytes;
        try {
            bodyBytes = objectMapper.writeValueAsBytes(mapped.result);
        } catch (Exception jsonEx) {
            // 极端兜底：序列化都失败时，手写最小 JSON
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            String fallback = "{\"code\":"
                    + ErrorCode.SERVER_ERROR.getCode()
                    + ",\"message\":\"Gateway internal error\",\"data\":null}";
            bodyBytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bodyBytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mapped mapException(Throwable ex) {

        // 1) 未匹配到路由（Gateway 常见：路径不匹配路由配置、服务未注册等）
        // org.springframework.cloud.gateway.support.NotFoundException
        if (isClass(ex, "org.springframework.cloud.gateway.support.NotFoundException")) {
            return mapped(
                    HttpStatus.NOT_FOUND,
                    Result.fail(ErrorCode.GATEWAY_NO_ROUTE.getCode(), "No route found")
            );
        }

        // 2) 限流：ResponseStatusException(429)
        if (ex instanceof ResponseStatusException rse) {
            if (rse.getStatusCode().value() == ErrorCode.TOO_MANY_REQUESTS.getCode()) {
                return mapped(
                        HttpStatus.TOO_MANY_REQUESTS,
                        Result.fail(ErrorCode.TOO_MANY_REQUESTS.getCode(), "Too many requests")
                );
            }
        }

        // 3) 超时：网关到下游请求超时
        if (ex instanceof TimeoutException
                || isClass(ex, "io.netty.handler.timeout.ReadTimeoutException")
                || isClass(ex, "reactor.netty.http.client.PrematureCloseException")) {
            return mapped(
                    HttpStatus.GATEWAY_TIMEOUT,
                    Result.fail(ErrorCode.GATEWAY_TIMEOUT.getCode(), "Gateway timeout")
            );
        }

        // 4) 下游不可用：连接失败、DNS 解析失败、无实例等
        if (ex instanceof ConnectException
                || ex instanceof UnknownHostException
                || isClass(ex, "org.springframework.cloud.client.loadbalancer.reactive.NoAvailableServiceInstanceException")) {
            return mapped(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    Result.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(), "Service unavailable")
            );
        }

        // 5) 兜底：网关内部错误
        return mapped(
                HttpStatus.INTERNAL_SERVER_ERROR,
                Result.fail(ErrorCode.SERVER_ERROR.getCode(), "Gateway internal error")
        );
    }

    private boolean isClass(Throwable ex, String className) {
        return ex != null && ex.getClass().getName().equals(className);
    }

    private Mapped mapped(HttpStatus httpStatus, Result<?> result) {
        return new Mapped(httpStatus, result);
    }

    private static class Mapped {
        final HttpStatus httpStatus;
        final Result<?> result;

        Mapped(HttpStatus httpStatus, Result<?> result) {
            this.httpStatus = httpStatus;
            this.result = result;
        }
    }
}
