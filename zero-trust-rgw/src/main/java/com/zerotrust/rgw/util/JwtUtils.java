package com.zerotrust.rgw.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.PublicKey;
import java.io.FileInputStream;

@Slf4j
@Component
public class JwtUtils {
    
   // private final SecretKey secretKey;
    private PublicKey publicKey;

    // public JwtUtils(@Value("${jwt.secret}") String secret) {
    //     byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    //     if (keyBytes.length < 32) {
    //         // 如果密钥不够长，进行填充
    //         byte[] paddedKey = new byte[32];
    //         System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
    //         keyBytes = paddedKey;
    //     }
    //     this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    // }
    
    // public Claims parseToken(String token) {
    //     return Jwts.parserBuilder()
    //             .setSigningKey(secretKey)
    //             .build()
    //             .parseClaimsJws(token)
    //             .getBody();
    // }

     // 在构造函数中加载 Casdoor 的证书
    public JwtUtils(@Value("${jwt.certificate-path}") String certPath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(certPath)) {
            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
            this.publicKey = cert.getPublicKey(); // 提取公钥
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey) // 【核心修改】传入公钥对象
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public String getUserId(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (Exception e) {
            log.warn("解析 userId 失败", e);
            return null;
        }
    }
    
    public String getResourceId(String token) {
        try {
            return parseToken(token).get("resourceId", String.class);
        } catch (Exception e) {
            log.warn("解析 resourceId 失败", e);
            return null;
        }
    }

    /**
     * 从 JWT 中解析 clientType（可选 claim，可能为空）
     */
    public String getClientType(String token) {
        try {
            return parseToken(token).get("clientType", String.class);
        } catch (Exception e) {
            log.debug("解析 clientType 失败（claim 可能不存在）", e);
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return false;
        }
    }
}