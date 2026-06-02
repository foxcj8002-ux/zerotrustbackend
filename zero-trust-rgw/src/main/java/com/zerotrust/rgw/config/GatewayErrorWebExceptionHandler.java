package com.zerotrust.rgw.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(-1) // 优先级必须高于默认的异常处理器
@RequiredArgsConstructor
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 1. 设置 Header 
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // 2. 根据异常类型映射状态码
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Internal Server Error";
        
        if (ex instanceof ResponseStatusException) {
            status = (HttpStatus) ((ResponseStatusException) ex).getStatusCode();
            message = ((ResponseStatusException) ex).getReason();
        } else if (ex.getMessage() != null && ex.getMessage().contains("Access Denied")) {
            status = HttpStatus.FORBIDDEN;
            message = ex.getMessage();
        }

        response.setStatusCode(status);

        // 3. 封装项目统一的 Result 对象 
        Map<String, Object> result = new HashMap<>();
        result.put("code", status.value());
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());

        // 4. 写回 JSON 字节流
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(result);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
