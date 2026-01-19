package com.zerotrust.trusteval.service;

import com.bisnode.opa.client.OpaClient;
import com.bisnode.opa.client.query.QueryForDocumentRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpaService {

    private final OpaClient opaClient;
    @Value("${opa.policy-path}")
    private String policyPath;

    public OpaService(OpaClient opaClient) {
        this.opaClient = opaClient;
    }

    /**     * 执行信任评分策略
     * @param fullInputData 上游微服务传入的完整 JSON (包含 snapshot
    Id, contexts 等)
     * @return 包含 trustscore 和原始 traceId 的最终结果
     */    public Map<String, Object> executePolicy(Map<String, Object> fullInputData) {
        // --- 1. 提取策略输入数据 (Payload) ---
        // 做强制类型转换，假设上游传来的格式是正确的 Map
        Map<String, Object> policyInput = (Map<String, Object>) fullInputData.get("contexts");
        if (policyInput == null) {
            throw new IllegalArgumentException("JSON 数据缺少关键字段: contexts");
        }
        // --- 2. 调用 OPA ---
        // SDK 会自动包装成 {"input": { "userIdentity": ..., "device": ... }}
        QueryForDocumentRequest request = new QueryForDocumentRequest(policyInput, policyPath);
        // 获取评分结果
        Map<String, Object> opaResult = opaClient.queryForDocument(request, Map.class);
        // --- 3. 组装最终返回结果 ---
        Map<String, Object> finalResult = new HashMap<>();
        // 放入 OPA 的计算结果
        if (opaResult != null) {
            finalResult.putAll(opaResult);
        }
        // (B) 放入需要透传的元数据      // 根据你的 JSON 结构，提取外层的 ID 字段
        safePut(finalResult, fullInputData,
                "snapshotId");
        safePut(finalResult, fullInputData, "traceId");
        safePut(finalResult, fullInputData, "snapshotTime");
        safePut(finalResult, fullInputData,  "userId");
        safePut(finalResult, fullInputData, "deviceId");
        safePut(finalResult, fullInputData,  "sessionId");

        return finalResult;
    }
    // 安全地从源 Map 取值放入目标 Map
    private void safePut(Map<String, Object> target, Map<String, Object> source, String key) {
        if (source.containsKey(key)) {            target.put(key, source.get(key));
        }
    }
}
