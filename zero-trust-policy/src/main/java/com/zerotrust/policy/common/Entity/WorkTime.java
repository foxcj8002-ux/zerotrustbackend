package com.zerotrust.policy.common.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_work_time")
public class WorkTime {

    @TableId
    private Long id;

    private Integer workStartHour;
    private Integer workEndHour;
    private Integer nightStartHour;
    private Integer nightEndHour;
    private Boolean enabled;
}