package com.zerotrust.policy.common.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("t_network_zone")
public class NetworkZone {
        @TableId
        private Long id;
        private String name;
        private String cidr;
        private String type;
        private String riskLevel;
        private Boolean enabled;

}
