package com.eos.order.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 配置类
 *
 * <p>配置流量控制、熔断降级规则。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class SentinelConfig {

    /**
     * 注册 SentinelResourceAspect，使 @SentinelResource 注解生效
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    /**
     * 初始化流控规则
     *
     * <p>生产环境建议从 Nacos 配置中心动态加载规则</p>
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 订单创建接口：QPS 限制为 100
        FlowRule orderCreateRule = new FlowRule();
        orderCreateRule.setResource("createOrder");
        orderCreateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        orderCreateRule.setCount(100);
        orderCreateRule.setLimitApp("default");
        rules.add(orderCreateRule);

        // 订单查询接口：QPS 限制为 500
        FlowRule orderQueryRule = new FlowRule();
        orderQueryRule.setResource("queryOrder");
        orderQueryRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        orderQueryRule.setCount(500);
        orderQueryRule.setLimitApp("default");
        rules.add(orderQueryRule);

        FlowRuleManager.loadRules(rules);
        log.info("[Sentinel] 流控规则初始化完成，规则数量={}", rules.size());
    }
}
