package com.zerotrust.collecting.common.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@Builder
//多维度安全上下文快照
public class SecurityContextSnapshot {
     private String requestId;          // 本次采集/访问的唯一标识
     private String userId;             // 用户ID（从Casdoor）
     private UserContext userContext; //用户身份上下文
     private DeviceContext deviceContext; //设备上下文
     private NetworkContext networkContext; //网络上下文
     private BehaviorContext behaviorContext;//行为上下文
     private EnvironmentContext environmentContext;//环境上下文
     private SessionContext sessionContext;//会话上下文
     @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
     private LocalDateTime collectedAt;       // 采集时间


}
