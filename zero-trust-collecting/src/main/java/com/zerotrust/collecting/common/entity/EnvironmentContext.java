package com.zerotrust.collecting.common.entity;

import lombok.Data;

@Data
public class EnvironmentContext {

    /** 当前访问请求的时间*/
    private String time;

    /** 源地址 */
    private String ip;

    /** 应用所在时区，方便 monitoring 做本地时间转换 */
    private String appTimeZone;    /**当前所在地理位置，与常用位置对比用于风险判定*/

    private String geoCity;      // 如 "Beijing"
    private String geoCountry;   // 如 "CN"

    /**
     * 威胁情报级别：先固定为None
     */
    private String threatIntelLevel;
}

