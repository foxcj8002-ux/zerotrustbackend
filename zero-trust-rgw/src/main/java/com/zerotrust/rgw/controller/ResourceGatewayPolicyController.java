package com.zerotrust.rgw.controller;

import com.zerotrust.rgw.model.PolicyDecision;
import com.zerotrust.rgw.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.server.reactive.SslInfo;
import org.springframework.util.StringUtils; // 使用 Spring 自带工具类，兼容性更好
import java.security.cert.X509Certificate;

@Slf4j
@RestController
@RequestMapping("/gateway/policy")
@RequiredArgsConstructor
public class ResourceGatewayPolicyController {

    private final PolicyService policyService;

    /**
     * 接收控制器下发的策略，返回确认
     */
    @PostMapping("/receive")
    public Mono<Map<String, Object>> receivePolicy(ServerWebExchange exchange, @RequestBody PolicyDecision decision) {
        // 执行合法性校验
        // // 1. 从 SSL 会话中提取客户端证书
        // SslInfo sslInfo = exchange.getRequest().getSslInfo();
        // if (sslInfo == null || sslInfo.getPeerCertificates() == null) {
        //     return Mono.just(Map.of("success", false, "message", "Missing mTLS Certificate"));
        // }

        // // 2. 获取证书对象
        // X509Certificate clientCert = (X509Certificate) sslInfo.getPeerCertificates()[0];

        // // 3. 校验证书的主体名 (Subject DN)
        // // 脚本里给控制器签发的证书 CN 是 "ZeroTrust-Controller"
        // String subjectDN = clientCert.getSubjectX500Principal().getName();

        // if (!subjectDN.contains("CN=ZeroTrust-Controller")) {
        //     log.error("非法访问企图！虽然持有合法证书但非控制器身份: {}", subjectDN);
        //     return Mono.just(Map.of("success", false, "message", "Identity Mismatch: Not a Controller"));
        // }

        // log.info("控制面身份认证通过: {}", subjectDN);

        // 校验核心字段是否缺失
        if (decision == null ||
                !StringUtils.hasText(decision.getSessionId()) || // 兼容 Java 8+
                !StringUtils.hasText(decision.getUserId()) ||
                !StringUtils.hasText(decision.getResourceId())) {

            log.error("收到非法策略报文: 关键字段缺失");
            Map<String, Object> errorMap = new HashMap<>(); // 保持可变 Map，即使只读
            errorMap.put("success", false);
            errorMap.put("message", "合法性校验失败：关键字段(SessionId/UserId/ResourceId)不能为空");
            return Mono.just(errorMap);
        }

        // 校验时间是否合法（假设过期时间也为必填）
        if (decision.getExpireTime() == null || decision.getExpireTime() < System.currentTimeMillis()) {
            log.error("收到非法策略报文: 策略已过期或缺失时间");
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("message", "合法性校验失败：策略已过期或缺失过期时间");
            return Mono.just(errorMap);
        }

        log.info("报文校验通过，准备写入Redis: sessionId={}", decision.getSessionId());

        // 写入 Redis
        return policyService.savePolicy(decision)
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    if (Boolean.TRUE.equals(success)) { // 防止 success 为 null
                        response.put("success", true);
                        response.put("message", "策略同步成功");
                        response.put("sessionId", decision.getSessionId());
                    } else {
                        response.put("success", false);
                        response.put("message", "策略存储失败：Redis写入异常");
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