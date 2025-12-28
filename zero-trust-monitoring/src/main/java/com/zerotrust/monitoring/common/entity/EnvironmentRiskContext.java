package com.zerotrust.monitoring.common.entity;

import lombok.Data;

@Data

public class EnvironmentRiskContext {

    private String time;            // 原始时间
    private String appTimeZone;     // 时区

    private String timeRiskLevel;   // WORK_TIME / OFF_TIME / NIGHT
    private String geoRiskLevel;    // KNOWN_LOCATION / UNKNOWN_CITY / UNKNOWN_COUNTRY
    private String threatIntelLevel;// NONE / SUSPICIOUS / KNOWN_THREAT
}
