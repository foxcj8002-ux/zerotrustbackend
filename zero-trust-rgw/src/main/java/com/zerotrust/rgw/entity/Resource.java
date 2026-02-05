package com.zerotrust.rgw.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("resource")  // R2DBC 使用这个注解
public class Resource {
    @Id
    private Long id;
    
    @Column("resource_id")
    private String resourceId;// 资源唯一标识
    
    @Column("resource_name")
    private String resourceName;// 资源名称
    
    @Column("resource_url")
    private String resourceUrl;// 资源内网地址
    
    @Column("resource_type")
    private String resourceType;// 资源类型
}