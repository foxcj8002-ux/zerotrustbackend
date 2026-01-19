package com.zerotrust.trusteval;

import com.zerotrust.trusteval.service.OpaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class ZeroTrustTrustevalApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private OpaService opaService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void testExecutePolicy_HighTrust() {
        // 1. 满分数据 (符合所有规则)
        Map<String, Object> inputData = createBaseData();
        Map<String, Object> contexts = (Map<String, Object>) inputData.get("contexts");

        contexts.put("networkType", "corporate");
        contexts.put("tlsVersion", "TLS1.3");
        contexts.put("country", "CN");
        contexts.put("city", "Beijing");
        contexts.put("ops10m", 100);
        contexts.put("bytesOut10m", 102400);

        // 2. 调用 Service
        Map<String, Object> result = opaService.executePolicy(inputData);
        // 3. 验证结果
        System.out.println("High Trust Result: " + result);

    }

    @Test
    void testExecutePolicy_LowTrust_WithPenalties() {
        // 1. 准备高危数据
        Map<String, Object> inputData = createBaseData();
        Map<String, Object> contexts = (Map<String, Object>) inputData.get("contexts");
        // 设置违规属性
        contexts.put("networkType", "public"); // 扣 20
        contexts.put("tlsVersion", "TLS1.0");  // 扣 10
        contexts.put("city", "Tokyo");         // 扣 15
        // 预期得分 55 分
        // 2. 调用 Service
        Map<String, Object> result = opaService.executePolicy(inputData);
        // 3. 验证结果
        System.out.println("Low Trust Result: " + result);

    }

    // --- 辅助方法：构造基础数据结构 ---
    private Map<String, Object> createBaseData() {
        Map<String, Object> data = new HashMap<>();
        data.put("snapshotId", "snap_test_01");
        data.put("traceId", "trace_001");
        data.put("snapshotTime", "2025-12-22T09:31:02");
        data.put("userId", 111);
        data.put("deviceId", "dev_abc123");
        data.put("sessionId", 2);
        Map<String, Object> contexts = new HashMap<>();
        data.put("contexts", contexts);
        return data;
    }

}
