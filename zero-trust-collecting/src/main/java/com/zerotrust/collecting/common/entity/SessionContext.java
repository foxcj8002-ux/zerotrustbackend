package com.zerotrust.collecting.common.entity;

import lombok.Data;

@Data
public class SessionContext {

    /** 当前会话持续时长（秒或毫秒） */
    private long duration;

    /** 会话期间的用户活动（高风险/中风险/低风险） */
    private String sessionActivity;

    /** 会话空闲时间，用于判断用户是否长时间未操作 */
    private long idleTime;
}

