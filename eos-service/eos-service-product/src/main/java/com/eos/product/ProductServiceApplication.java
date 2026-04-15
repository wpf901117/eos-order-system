package com.eos.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 商品服务启动类
 *
 * @author EOS Team
 * @since 1.0.0
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.eos.product", "com.eos.common"})
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
