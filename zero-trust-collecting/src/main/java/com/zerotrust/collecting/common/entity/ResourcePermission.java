package com.zerotrust.collecting.common.entity;

import lombok.Data;

@Data
public class ResourcePermission {
    /**
     * 资源唯一标识ID
     */
    private String resourceId;
    /**
     * 资源所属域（domain），通常用于标识外部系统或业务域。
     * 例如：finance-system、hr-system、devops-system 等。
     */
    private String resourceDomain;

    /**
     * URL 访问范围（scope），可使用通配符表示一个目录或模块。
     * 例如：/report/** 表示允许访问报告模块下的所有资源。
     */
    private String scope;

    /**
     * 操作/权限动作，例如 read、write、delete、execute、access 等。
     * 用于描述允许用户在 scope 范围内执行哪些操作。
     */
    private String action;
}
