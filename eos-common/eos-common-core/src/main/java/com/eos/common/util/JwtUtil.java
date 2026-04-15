package com.eos.common.util;

import com.eos.common.constant.CommonConstant;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 *
 * <p>JWT（JSON Web Token）是目前主流的轻量级身份认证方案，特别适用于分布式微服务架构。
 * 相比传统的Session方案，JWT具有以下优势：</p>
 * <ul>
 *   <li><strong>无状态</strong>：服务端无需存储Session，适合水平扩展</li>
 *   <li><strong>自包含</strong>：Token中携带了用户基本信息，减少数据库查询</li>
 *   <li><strong>跨服务</strong>：网关校验Token后即可将用户信息透传给下游服务</li>
 * </ul>
 *
 * <p><strong>JWT结构：</strong></p>
 * <pre>
 * eyJhbGciOiJIUzI1NiJ9.  ← Header（算法信息）
 * eyJ1c2VySWQiOjF9.       ← Payload（用户数据）
 * SflKxwRJSMeKKF2QT4fwpMe... ← Signature（签名）
 * </pre>
 *
 * <p><strong>安全注意事项：</strong></p>
 * <ul>
 *   <li>密钥（secretKey）必须足够复杂且安全存储</li>
 *   <li>Token有效期不宜过长（建议AccessToken 15分钟-2小时）</li>
 *   <li>敏感信息不要放入Payload（Base64可解码）</li>
 *   <li>提供Token刷新机制（RefreshToken）</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
public class JwtUtil {

    /**
     * 密钥长度至少256位（32字节）
     * 生产环境应从配置中心或密钥管理系统获取
     */
    private static final String SECRET = "eos-enterprise-order-system-secret-key-32bytes";

    /** AccessToken有效期：30分钟 */
    private static final long ACCESS_TOKEN_EXPIRE = 30 * 60 * 1000L;

    /** Token类型：访问令牌 */
    public static final String TOKEN_TYPE_ACCESS = "access";

    /** Token类型：刷新令牌 */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /** RefreshToken有效期：7天 */
    private static final long REFRESH_TOKEN_EXPIRE = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 生成安全的HMAC-SHA256密钥
     */
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成访问令牌
     *
     * @param claims 声明内容，如userId、roles等
     * @param sid    会话ID（Session ID，用于session撤销）
     * @return JWT字符串
     */
    public static String generateAccessToken(Map<String, Object> claims, String sid) {
        claims.put("tokenType", TOKEN_TYPE_ACCESS);
        claims.put("sid", sid);
        return generateToken(claims, ACCESS_TOKEN_EXPIRE);
    }

    /**
     * 生成刷新令牌
     *
     * @param claims 声明内容
     * @param sid    会话ID
     * @return JWT字符串
     */
    public static String generateRefreshToken(Map<String, Object> claims, String sid) {
        claims.put("tokenType", TOKEN_TYPE_REFRESH);
        claims.put("sid", sid);
        return generateToken(claims, REFRESH_TOKEN_EXPIRE);
    }

    /**
     * 生成JWT Token
     *
     * @param claims     自定义声明
     * @param expiration 过期时间（毫秒）
     * @return JWT字符串
     */
    public static String generateToken(Map<String, Object> claims, long expiration) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expireTime)
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析JWT Token
     *
     * @param token JWT字符串
     * @return Claims对象
     * @throws JwtException Token解析失败或已过期
     */
    public static Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT字符串（不含Bearer前缀）
     * @return true 如果Token有效且未过期
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token格式");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误");
            return false;
        } catch (SignatureException e) {
            log.warn("Token签名验证失败");
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token为空或非法");
            return false;
        }
    }

    /**
     * 获取Token过期时间
     *
     * @param token JWT字符串
     * @return 过期时间
     */
    public static Date getExpirationDate(String token) {
        return parseToken(token).getExpiration();
    }

    /**
     * 获取Token剩余有效时间（毫秒）
     *
     * @param token JWT字符串
     * @return 剩余毫秒数，已过期返回0
     */
    public static long getRemainingTime(String token) {
        Date expiration = getExpirationDate(token);
        long remaining = expiration.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    /**
     * 从请求头中提取Token
     *
     * @param authHeader Authorization请求头
     * @return 纯Token字符串，如果不含Bearer前缀则原样返回
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(CommonConstant.TOKEN_PREFIX)) {
            return authHeader.substring(CommonConstant.TOKEN_PREFIX.length());
        }
        return authHeader;
    }

    /**
     * 获取Token类型
     *
     * @param token JWT字符串
     * @return tokenType（access/refresh），无type字段时返回null
     */
    public static String getTokenType(String token) {
        try {
            Claims claims = parseToken(token);
            Object type = claims.get("tokenType");
            return type != null ? type.toString() : null;
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * 获取会话ID
     *
     * @param token JWT字符串
     * @return sid，无sid字段时返回null
     */
    public static String getSessionId(String token) {
        try {
            Claims claims = parseToken(token);
            Object sid = claims.get("sid");
            return sid != null ? sid.toString() : null;
        } catch (JwtException e) {
            return null;
        }
    }
}
