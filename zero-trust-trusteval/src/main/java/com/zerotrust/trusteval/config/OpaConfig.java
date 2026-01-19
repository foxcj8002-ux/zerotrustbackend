package com.zerotrust.trusteval.config;

import com.bisnode.opa.client.OpaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;


@Configuration
public class OpaConfig {

    @Value("${opa.base-url}")
    private String opaBaseUrl;

    @Bean
    public OpaClient opaClient() {

        return OpaClient.builder()
                .opaConfiguration(opaBaseUrl)
                .build();
    }

}
