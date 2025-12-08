package com.zerotrust.monitoring.common.entity;

import lombok.Data;

@Data
//多维度安全上下文
public class SecurityContext {
    private UserContext userContext; //用户身份上下文
    private DeviceContext deviceContext; //设备上下文
    private NetworkContext networkContext; //网络上下文
    private BehaviorContext behaviorContext;//行为上下文
    private EnvironmentContext environmentContext;//环境上下文
    private SessionContext sessionContext;//会话上下文
}
