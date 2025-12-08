package com.zerotrust.collecting.common.entity;


import lombok.Data;

import java.util.List;

/**
 * 设备安全上下文，描述用户本次请求使用的设备及其安全状态。
 */
@Data
public class DeviceContext {

    /** 设备唯一硬件标识（指纹） */
    private String deviceId;

    /** 设备类型，如 desktop、mobile、server */
    private String deviceType;

    /** 操作系统版本 */
    private String osVersion;

    /** 操作系统补丁级别 */
    private String patchLevel;

    /** 设备防火墙是否开启 */
    private boolean firewallEnabled;

    /** 磁盘是否加密 */
    private boolean diskEncrypted;

    /** 是否存在 Root / 越狱行为 */
    private boolean rooted;

    /** 已安装的软件列表 */
    private List<String> installedSoftware;

    /** 设备当前 IP 地址或网络段 */
    private String networkLocation;

    /** 设备是否注册并符合安全策略 */
    private boolean compliant;
}
