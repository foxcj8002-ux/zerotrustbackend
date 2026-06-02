package com.zerotrust.rgw.config;

import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.server.HttpServer;

/**
 * 强行覆盖 Netty 底层 Header 限制的配置类
 */
@Configuration
public class NettyConfiguration {

    @Bean
    public NettyServerCustomizer nettyServerCustomizer() {
        return httpServer -> httpServer.httpRequestDecoder(
            // 强行设置为 64KB (65536字节)
            spec -> spec.maxHeaderSize(65536)
                        .maxInitialLineLength(65536)
        );
    }
}