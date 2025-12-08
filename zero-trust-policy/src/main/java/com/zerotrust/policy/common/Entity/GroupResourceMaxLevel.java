package com.zerotrust.policy.common.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
@Data
@TableName("t_group_resource_max_level")
public class GroupResourceMaxLevel {

    @TableId
    private Long id;

    private Long groupId;
    private String targetType;      // RESOURCE / RESOURCE_GROUP
    private Long resourceId;
    private Long resourceGroupId;
    private String maxLevelCode;    // 用四个等级
    private Boolean enabled;
}
