package com.eos.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 配置类
 *
 * <p>MyBatis Plus 是 MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变。
 * 本配置类集中管理 MP 的核心扩展功能：</p>
 *
 * <ul>
 *   <li><strong>分页插件</strong>：自动拦截 SQL 并拼接分页参数</li>
 *   <li><strong>自动填充</strong>：插入/更新时自动设置 create_time 和 update_time</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis Plus 拦截器
     *
     * <p>MP 3.4.0+ 版本采用新的插件体系，通过 {@link MybatisPlusInterceptor} 统一管理所有插件。</p>
     *
     * <p><strong>分页插件原理：</strong></p>
     * <ol>
     *   <li>拦截 Executor.query 方法</li>
     *   <li>解析原始 SQL，包装成 COUNT 查询获取总数</li>
     *   <li>再在原 SQL 后追加 LIMIT 分页条件</li>
     * </ol>
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件：指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        log.info("[MyBatis Plus] 分页插件已注册，数据库类型：MYSQL");
        return interceptor;
    }

    /**
     * 元对象字段填充控制器
     *
     * <p>通过 @TableField(fill = FieldFill.INSERT) 和
     * @TableField(fill = FieldFill.INSERT_UPDATE) 注解，
     * 实现创建时间和更新时间的自动填充，无需在业务代码中手动设置。</p>
     *
     * <p>这是《阿里巴巴Java开发手册》推荐的优雅做法，可以：</p>
     * <ul>
     *   <li>减少重复代码</li>
     *   <li>避免人为遗漏</li>
     *   <li>保证时间一致性</li>
     * </ul>
     *
     * @return MetaObjectHandler 实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                log.debug("[自动填充] INSERT 操作，设置 createTime 和 updateTime");
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                log.debug("[自动填充] UPDATE 操作，设置 updateTime");
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
