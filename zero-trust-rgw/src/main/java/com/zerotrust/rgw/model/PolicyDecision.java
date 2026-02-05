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
}