package com.zerotrust.rgw.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Data
@Component
@ConfigurationProperties(prefix = "gateway.safety")
@RefreshScope // 让 Nacos 的配置变更能实时推送到这个类里
public class GatewaySafetyProperties {

    /**
     * SSRF 访问白名单
     * Spring 会自动将 YAML 中逗号分隔的字符串或列表转换为 List<String>
     */
    private List<String> allowedHosts = new ArrayList<>();

}