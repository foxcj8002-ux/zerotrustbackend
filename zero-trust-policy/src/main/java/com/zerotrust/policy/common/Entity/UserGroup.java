package com.zerotrust.policy.common.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_user_group")
public class UserGroup {
    @TableId
    private Long id;
    private String userId;
    private Long groupId;
}
