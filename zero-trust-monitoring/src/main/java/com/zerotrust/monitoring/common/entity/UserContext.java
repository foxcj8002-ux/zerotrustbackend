package com.zerotrust.monitoring.common.entity;

import lombok.Data;

import java.util.List;

@Data
public class UserContext {
    /** 用户唯一标识 ID */
    private String userId;

    /** 是否已通过身份认证 */
    private boolean authenticated;

    /** 用户角色（如 admin、developer、user 等） */
    private String role;

    /** 最近一次认证时间（时间戳 ms） */
    private long lastAuthTime;

    /** 认证强度（1=弱密码，2=强密码，3=短信验证，4=扫码登录等） */
    private int authStrength;

    /** 认证频率，用于判断近期是否频繁登录 */
    private int authFrequency;

    /**
     * 用户的范围权限列表，用于标识用户可访问的资源范围
     * 每一项描述用户对某外部系统（domain）下某 URL 范围（scope）的权限动作（action）。
     * 例如：
     *  001 finance-system   /report/**      read
     *  002 hr-system        /profile/**     write
     */
    List<ResourcePermission> permissions;
}
