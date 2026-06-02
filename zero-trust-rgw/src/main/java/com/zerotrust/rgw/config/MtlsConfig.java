package com.zerotrust.rgw.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory; 

@Slf4j
@Configuration
public class MtlsConfig {
    
    @Value("${gateway.mtls.enabled:false}")
    private boolean mtlsEnabled;
    
    @Value("${gateway.mtls.key-store-path:}")
    private String keyStorePath;
    
    @Value("${gateway.mtls.key-store-password:}")
    private String keyStorePassword;
    
    @Value("${gateway.mtls.trust-store-path:}")
    private String trustStorePath;
    
    @Value("${gateway.mtls.trust-store-password:}")
    private String trustStorePassword;
    
    /**
     * 自定义 HttpClient，启用 mTLS
     */
    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return httpClient -> {
            if (!mtlsEnabled) {
                log.info("mTLS 未启用，使用默认 HTTP 客户端");
                return httpClient;
            }
            
            try {
                SslContext sslContext = buildSslContext();
                log.info("mTLS 已启用，证书加载成功");
                return httpClient.secure(spec -> spec.sslContext(sslContext));
            } catch (Exception e) {
                log.error("mTLS 配置失败，回退到默认配置", e);
                return httpClient;
            }
        };

    }
    
    private SslContext buildSslContext() throws Exception {
        // 加载客户端密钥库（网关的身份证书）
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(new File(keyStorePath))) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }
        
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
        
        // 加载信任库（信任的 CA 证书）
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(new File(trustStorePath))) {
            trustStore.load(fis, trustStorePassword.toCharArray());
        }
        
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        
        // 构建 SSL 上下文
        return SslContextBuilder.forClient()
                .keyManager(keyManagerFactory)
                //.trustManager(trustManagerFactory)
                .trustManager(InsecureTrustManagerFactory.INSTANCE) // 仅用于测试环境，生产环境请使用 trustManagerFactory
                .build();
    }
}