package com.zerotrust.rgw.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtils {
    
    private final SecretKey secretKey;
    
    public JwtUtils(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()         //构建解析器
                .setSigningKey(secretKey)   //设置签名密钥
                .build()                    //生成解析器
                .parseClaimsJws(token)      //解析JWT
                .getBody();                 //获取载荷
    }
    
    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }
    
    public String getResourceId(String token) {
        return parseToken(token).get("resourceId", String.class);
    }
    
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}