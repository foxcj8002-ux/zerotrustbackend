package com.zerotrust.policy.common.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_effective_permission")
public class UserEffectivePermission {

    @TableId
    private Long id;

    private String userId;
    private Long groupId;
    private Long resourceGroupId;
    private String effectiveLevel;   // 存 PermissionLevel.name()
    private Integer sourceScore;
    private LocalDateTime updatedAt;
}
