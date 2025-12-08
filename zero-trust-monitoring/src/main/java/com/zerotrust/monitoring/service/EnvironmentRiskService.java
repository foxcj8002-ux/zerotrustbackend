//package com.zerotrust.collecting.monitoring.service;
//
//import com.zerotrust.collecting.common.entity.EnvironmentContext;
//import com.zerotrust.collecting.monitoring.common.entity.EnvironmentRiskContext;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
//@Service
//@RequiredArgsConstructor
//public class EnvironmentRiskService {
//
//    private final EnvironmentRiskProperties properties; // 工作时间配置
//
//    public EnvironmentRiskContext buildRisk(EnvironmentContext envRaw, String userId) {
//        EnvironmentRiskContext risk = new EnvironmentRiskContext();
//        risk.setTime(envRaw.getTime());
//        risk.setAppTimeZone(envRaw.getAppTimeZone());
//
//        risk.setTimeRiskLevel(calcTimeRiskLevel(envRaw));
//        risk.setGeoRiskLevel(calcGeoRiskLevel(envRaw, userId));
//        risk.setThreatIntelLevel(calcThreatIntelLevel(envRaw));
//
//        return risk;
//    }
//
//    private String calcTimeRiskLevel(EnvironmentContext envRaw) {
//        LocalDateTime time = LocalDateTime.parse(envRaw.getTime(),
//                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//
//        int hour = time.getHour();
//        int workStart = properties.getWorkStartHour(); // 例如 9
//        int workEnd = properties.getWorkEndHour();     // 例如 18
//        int nightStart = properties.getNightStartHour(); // 23
//        int nightEnd = properties.getNightEndHour();     // 6
//
//        if (nightStart <= nightEnd) {
//            if (hour >= nightStart && hour < nightEnd) {
//                return "NIGHT";
//            }
//        } else {
//            if (hour >= nightStart || hour < nightEnd) {
//                return "NIGHT";
//            }
//        }
//
//        if (hour >= workStart && hour < workEnd) {
//            return "WORK_TIME";
//        }
//        return "OFF_TIME";
//    }
//
//    // calcGeoRiskLevel / calcThreatIntelLevel 下面讲
//}
//
