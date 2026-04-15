package com.eos.gateway.filter;

import com.eos.common.constant.CommonConstant;
import com.eos.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 网关统一认证过滤器
 *
 * <p>这是微服务安全架构的核心组件，负责对所有进入系统的请求进行JWT认证。
 * 通过网关统一鉴权，下游服务无需重复实现认证逻辑。</p>
 *
 * <p><strong>放行白名单：</strong>不需要Token就能访问的接口</p>
 * <ul>
 *   <li>用户注册</li>
 *   <li>用户登录</li>
 *   <li>商品列表/详情</li>
 *   <li>健康检查</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    /** 路径匹配器 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 白名单：无需认证的接口 */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/register",
            "/user/login",
            "/user/refresh",
            "/product/list",
            "/actuator/**",
            "/health"
    );

    /**
     * 过滤逻辑
     *
     * @param exchange ServerWebExchange
     * @param chain    GatewayFilterChain
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取Token
        String authHeader = request.getHeaders().getFirst(CommonConstant.HEADER_TOKEN);
        if (authHeader == null || !authHeader.startsWith(CommonConstant.TOKEN_PREFIX)) {
            log.warn("[网关认证] 请求缺少Token，path={}", path);
            return unauthorized(exchange.getResponse(), "缺少认证信息");
        }

        String token = JwtUtil.extractToken(authHeader);

        // 3. 校验Token是否有效
        if (!JwtUtil.validateToken(token)) {
            log.warn("[网关认证] Token无效或已过期");
            return unauthorized(exchange.getResponse(), "Token无效或已过期");
        }

        // 3.1 拒绝refresh token访问业务接口
        String tokenType = JwtUtil.getTokenType(token);
        if (JwtUtil.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            log.warn("[网关认证] Refresh Token不能用于访问业务接口");
            return unauthorized(exchange.getResponse(), "Refresh Token不能用于访问业务接口");
        }

        // 3.2 检查Session是否已撤销
        String sessionId = JwtUtil.getSessionId(token);
        if (sessionId != null) {
            String sessionKey = CommonConstant.CACHE_SESSION + sessionId;
            return reactiveRedisTemplate.hasKey(sessionKey)
                    .flatMap(sessionExists -> {
                        if (!Boolean.TRUE.equals(sessionExists)) {
                            log.warn("[网关认证] Session已撤销，sessionId={}", sessionId);
                            return unauthorized(exchange.getResponse(), "会话已失效，请重新登录");
                        }

                        // 4. 解析Token并将用户信息透传给下游服务
                        Claims claims = JwtUtil.parseToken(token);
                        String userId = claims.get("userId", String.class);
                        String username = claims.get("username", String.class);
                        String role = claims.get("role", String.class);

                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header(CommonConstant.HEADER_USER_ID, userId)
                                .header(CommonConstant.HEADER_USERNAME, username)
                                .header(CommonConstant.HEADER_ROLE, role)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        }

        // 无sid的旧token（兼容路径，仅做黑名单检查）
        String blacklistKey = CommonConstant.CACHE_TOKEN_BLACKLIST + token;
        return reactiveRedisTemplate.hasKey(blacklistKey)
                .flatMap(inBlacklist -> {
                    if (Boolean.TRUE.equals(inBlacklist)) {
                        log.warn("[网关认证] Token已被登出");
                        return unauthorized(exchange.getResponse(), "Token已失效");
                    }

                    Claims claims = JwtUtil.parseToken(token);
                    String userId = claims.get("userId", String.class);
                    String username = claims.get("username", String.class);
                    String role = claims.get("role", String.class);

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header(CommonConstant.HEADER_USER_ID, userId)
                            .header(CommonConstant.HEADER_USERNAME, username)
                            .header(CommonConstant.HEADER_ROLE, role)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    /**
     * 过滤器优先级
     *
     * @return 顺序值，越小优先级越高
     */
    @Override
    public int getOrder() {
        return -100; // 确保在其他过滤器之前执行
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhiteList(String path) {
        if (WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return true;
        }
        return path.matches("^/product/\\d+$") || path.matches("^/product/\\d+/stock$");
    }

    /**
     * 返回401未认证响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message, System.currentTimeMillis());
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
