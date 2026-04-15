package com.eos.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类
 *
 * <p>Spring Security 是 Java 生态中最强大的安全框架。在微服务架构中，
 * 通常只在用户服务中使用 Security 进行密码加密，而认证授权逻辑放在网关层统一处理。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置密码加密器
     *
     * <p>BCrypt 是目前最推荐的密码哈希算法：</p>
     * <ul>
     *   <li>自带随机 salt，无需单独存储</li>
     *   <li>可以设置工作因子（默认10），迭代次数=2^10</li>
     *   <li>抗彩虹表攻击和暴力破解</li>
     * </ul>
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤链
     *
     * <p>用户服务不处理认证逻辑，所以禁用 Session 和 CSRF，
     * 允许所有请求通过，由网关层统一鉴权。</p>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（前后端分离通常不需要）
            .csrf().disable()
            // 不使用 Session
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 放行所有请求
            .authorizeRequests().anyRequest().permitAll();

        return http.build();
    }
}
