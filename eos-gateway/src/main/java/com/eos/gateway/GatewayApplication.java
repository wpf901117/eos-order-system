package com.eos.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API网关启动类
 *
 * <p>网关是微服务架构的统一入口，所有外部请求都必须经过网关。网关的核心职责：</p>
 * <ul>
 *   <li><strong>路由转发</strong>：根据URL路径将请求转发到对应的服务</li>
 *   <li><strong>统一鉴权</strong>：JWT验证、权限校验</li>
 *   <li><strong>限流熔断</strong>：Sentinel保护后端服务</li>
 *   <li><strong>日志监控</strong>：统一记录请求日志</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.eos.gateway", "com.eos.common"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
