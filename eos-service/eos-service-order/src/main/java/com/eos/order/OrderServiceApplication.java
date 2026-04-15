package com.eos.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类
 *
 * @author EOS Team
 * @since 1.0.0
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.eos.order.feign")
@EnableScheduling  // 启用定时任务
@SpringBootApplication(scanBasePackages = {"com.eos.order", "com.eos.common"})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
