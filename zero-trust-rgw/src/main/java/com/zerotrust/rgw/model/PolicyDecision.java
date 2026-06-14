package com.zerotrust.rgw.model;

import lombok.Data;

@Data
public class PolicyDecision {
    private String sessionId;//会话标识
    private String userId;//用户标识
    private String resourceId;//资源标识
    private String action;//操作类型
    private boolean allowed;//是否允许
    private Long expireTime;//过期时间

    // === 多端接入新增字段 ===
    /** 客户端类型：USER / API_CLIENT / SOFTWARE_TOOL */
    private String clientType;
    /** 设备指纹，仅 SOFTWARE_TOOL 必填 */
    private String deviceFingerprint;
}