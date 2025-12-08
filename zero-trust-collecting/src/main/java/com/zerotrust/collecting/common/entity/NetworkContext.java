package com.zerotrust.collecting.common.entity;

import lombok.Data;

@Data
public class NetworkContext {

    /** 网络类型：corporate、publicWiFi、homeNetwork 等 */
    private String networkType;

    /** 网络连接加密协议，如 TLS1.2 / TLS1.3 */
    private String tlsVersion;

    /** 当前连接流量模式（是否异常） */
    private String trafficPattern;

    /** 网络来源地理位置（城市或国家） */
    private String geoLocation;
}

