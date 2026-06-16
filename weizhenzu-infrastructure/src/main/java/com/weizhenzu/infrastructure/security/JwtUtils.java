package com.weizhenzu.infrastructure.security;

import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret:weizhenzu-secret-key-must-be-at-least-32-bytes-long}")
    private String secret;

    @Value("${jwt.expire:7200}")
    private long expireSeconds;

    @Value("${jwt.refresh-expire:604800}")
    private long refreshExpireSeconds;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌
     */
    public String generate(Long userId, Integer userType, Map<String, Object> extra) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userType", userType)
                .claim("type", "access")
                .claims(extra)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key())
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefresh(Long userId, Integer userType) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + refreshExpireSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userType", userType)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expire)
                .signWith(key())
                .compact();
    }

    /**
     * 解析令牌
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "Token已过期");
        } catch (JwtException e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "Token无效");
        }
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }
}
