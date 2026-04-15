package com.eos.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 自动配置类
 *
 * <p>启用 AspectJ 自动代理，确保切面（幂等性、限流）正常工作。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackages = "com.eos.common.aspect")
public class AopAutoConfiguration {
}
