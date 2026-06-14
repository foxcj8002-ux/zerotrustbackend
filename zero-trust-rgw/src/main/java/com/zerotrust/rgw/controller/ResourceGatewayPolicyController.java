package com.zerotrust.rgw.controller;

import com.zerotrust.rgw.enums.ClientType;
import com.zerotrust.rgw.model.PolicyDecision;
import com.zerotrust.rgw.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

@Slf4j
@RestController
@RequestMapping("/gateway/policy")
@RequiredArgsConstructor
public class ResourceGatewayPolicyController {

    private final PolicyService policyService;

    /**
     * 接收控制器下发的策略，返回确认。
     *
     * 强制前提：在启用 mTLS 双向认证之前，不应将此接口暴露在公网。
     * mTLS 校验代码已就绪，配置好证书后可接入。
     */
    @PostMapping("/receive")
    public Mono<Map<String, Object>> receivePolicy(ServerWebExchange exchange,
                                                   @RequestBody PolicyDecision decision) {
        if (decision == null
                || !StringUtils.hasText(decision.getSessionId())
                || !StringUtils.hasText(decision.getUserId())
                || !StringUtils.hasText(decision.getResourceId())) {
            log.error("收到非法策略报文: 关键字段缺失");
            return Mono.just(Map.of(
                    "success", false,
                    "message", "关键字段不能为空: sessionId / userId / resourceId"
            ));
        }

        if (!StringUtils.hasText(decision.getAction())) {
            log.error("收到非法策略报文: action 不能为空");
            return Mono.just(Map.of(
                    "success", false,
                    "message", "action 不能为空"
            ));
        }
        if (!policyService.isAllowedAction(decision.getAction())) {
            log.error("收到非法策略报文: action 非法, action={}", decision.getAction());
            return Mono.just(Map.of(
                    "success", false,
                    "message", "action 非法，仅支持: GET / POST / PUT / DELETE / PATCH"
            ));
        }

        if (decision.getExpireTime() == null
                || decision.getExpireTime() <= System.currentTimeMillis()) {
            log.error("收到非法策略报文: expireTime 必须大于当前时间");
            return Mono.just(Map.of(
                    "success", false,
                    "message", "expireTime 必须为毫秒级时间戳，且大于当前时间"
            ));
        }

        if (!StringUtils.hasText(decision.getClientType())) {
            log.error("clientType 不能为空");
            return Mono.just(Map.of(
                    "success", false,
                    "message", "clientType 不能为空"
            ));
        }
        if (!ClientType.isValid(decision.getClientType())) {
            log.error("clientType 值非法: {}", decision.getClientType());
            return Mono.just(Map.of(
                    "success", false,
                    "message", "clientType 值非法，仅支持: USER / API_CLIENT / SOFTWARE_TOOL"
            ));
        }

        if (ClientType.SOFTWARE_TOOL.name().equals(decision.getClientType())
                && !StringUtils.hasText(decision.getDeviceFingerprint())) {
            log.error("SOFTWARE_TOOL 类型缺少 deviceFingerprint");
            return Mono.just(Map.of(
                    "success", false,
                    "message", "SOFTWARE_TOOL 类型必须提供 deviceFingerprint"
            ));
        }

        log.info("策略报文校验通过，准备写入 Redis: sessionId={}, clientType={}, allowed={}",
                decision.getSessionId(), decision.getClientType(), decision.isAllowed());

        return policyService.savePolicy(decision)
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    if (Boolean.TRUE.equals(success)) {
                        response.put("success", true);
                        response.put("message", "策略同步成功");
                        response.put("sessionId", decision.getSessionId());
                        response.put("allowed", decision.isAllowed());
                    } else {
                        response.put("success", false);
                        response.put("message", "策略存储失败：Redis 写入异常或策略已过期");
                    }
                    return response;
                });
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP"));
    }
}
