package com.zerotrust.policy.common.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_role_group")
public class RoleGroup {
    @TableId
    private Long id;
    private String groupName;
    private String description;
    private Boolean enabled;
}

