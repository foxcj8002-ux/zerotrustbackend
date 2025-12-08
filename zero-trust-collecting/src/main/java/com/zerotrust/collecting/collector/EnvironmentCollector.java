package com.zerotrust.collecting.collector;
import com.zerotrust.collecting.common.entity.EnvironmentContext;
import com.zerotrust.collecting.common.entity.SecurityContextSnapshot;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class EnvironmentCollector {

    // 时间格式保持一致
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 采集环境信息：当前时间 + 源IP → 地理信息（先用简单模拟）
     */
    public void collect(SecurityContextSnapshot snapshot, HttpServletRequest request) {
        EnvironmentContext env = new EnvironmentContext();

        // 1. 当前访问时间
        env.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        // 2. 源 IP（从请求中提取）
        String ip = getClientIp(request);

        // 3. 根据 IP 简单映射地理信息（先写死/模拟，后面可以换成 GeoIP）
        // 这里为了演示，给几个例子，你可以自己改：
        if (ip.startsWith("10.")) {
            env.setGeoCity("Beijing");
            env.setGeoCountry("CN");
        } else if (ip.startsWith("172.16.")) {
            env.setGeoCity("Shanghai");
            env.setGeoCountry("CN");
        } else {
            env.setGeoCity("Unknown City");
            env.setGeoCountry("Unknown");
        }

        // 4. 威胁情报等级：采集阶段先不做判断，固定为 NONE
        env.setThreatIntelLevel("NONE");

        // 5. 填回快照
        snapshot.setEnvironmentContext(env);

    }

    /*** 统一从请求中拿客户端 IP（*/
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // 处理多个IP的情况（如X-Forwarded-For: client, proxy1, proxy2）
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                if (isValidIp(ip)) {
                    return ip;
                }
            }
        }
        return request.getRemoteAddr();
    }
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        // 基础IP格式验证
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern);
    }

}
