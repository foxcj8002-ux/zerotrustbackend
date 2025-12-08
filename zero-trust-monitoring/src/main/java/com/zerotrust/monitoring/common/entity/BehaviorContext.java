package com.zerotrust.monitoring.common.entity;

import lombok.Data;

@Data
public class BehaviorContext {

    /** 用户操作类型（read / write / delete / execute 等） */
    private String operationType;

    /** 用户常见登录时间（早/中/晚/节假日等） */
    private String loginTimePattern;

    /** 用户访问的资源类型（敏感/常规/受限等） */
    private String resourceType;

    /** 单位时间内的操作频率，用于检测行为异常 */
    private int operationFrequency;

    /** 数据访问模式（正常/偏离/异常） */
    private String dataAccessPattern;
}
