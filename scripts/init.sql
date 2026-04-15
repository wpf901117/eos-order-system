-- =====================================
-- EOS 企业级订单管理系统 - 数据库初始化脚本
-- =====================================
-- 说明：
-- 1. 本脚本创建3个业务数据库：eos_user、eos_product、eos_order
-- 2. 每个库独立部署，模拟微服务的数据库拆分
-- 3. 表设计遵循阿里巴巴Java开发手册规范
-- =====================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS eos_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eos_product CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eos_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eos_user;

-- =====================================
-- 用户库：t_user 用户表
-- =====================================
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint(20) NOT NULL COMMENT '用户ID，雪花算法生成',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码，BCrypt加密',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `nickname` varchar(32) DEFAULT NULL COMMENT '昵称',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
  `role` varchar(16) NOT NULL DEFAULT 'USER' COMMENT '角色',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入测试用户（密码：123456，BCrypt加密后）
INSERT INTO `t_user` (`id`, `username`, `password`, `phone`, `nickname`, `status`, `role`, `create_time`)
VALUES
  (1000000000000000001, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '138****0001', '管理员', 1, 'ADMIN', NOW()),
  (1000000000000000002, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '138****0002', '张三', 1, 'USER', NOW()),
  (1000000000000000003, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '138****0003', '李四', 1, 'USER', NOW());


USE eos_product;

-- =====================================
-- 商品库：t_product 商品表
-- =====================================
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product` (
  `id` bigint(20) NOT NULL COMMENT '商品ID，雪花算法生成',
  `name` varchar(128) NOT NULL COMMENT '商品名称',
  `description` text COMMENT '商品描述',
  `price` decimal(12,2) NOT NULL COMMENT '商品价格',
  `stock` int(11) NOT NULL DEFAULT '0' COMMENT '库存数量',
  `image_url` varchar(255) DEFAULT NULL COMMENT '商品图片URL',
  `category_id` bigint(20) DEFAULT NULL COMMENT '分类ID',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：0-下架，1-上架',
  `sales` int(11) NOT NULL DEFAULT '0' COMMENT '销量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 插入测试商品
INSERT INTO `t_product` (`id`, `name`, `description`, `price`, `stock`, `status`, `sales`, `create_time`)
VALUES
  (2000000000000000001, 'iPhone 15 Pro', 'Apple最新旗舰手机', 7999.00, 100, 1, 0, NOW()),
  (2000000000000000002, 'MacBook Pro 14', 'M3芯片专业笔记本', 14999.00, 50, 1, 0, NOW()),
  (2000000000000000003, 'AirPods Pro 2', '主动降噪无线耳机', 1899.00, 200, 1, 0, NOW()),
  (2000000000000000004, '小米14', '徕卡影像旗舰', 3999.00, 150, 1, 0, NOW()),
  (2000000000000000005, '华为Mate 60 Pro', '卫星通信旗舰', 6999.00, 80, 1, 0, NOW());


USE eos_order;

-- =====================================
-- 订单库：t_order 订单表
-- =====================================
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `id` bigint(20) NOT NULL COMMENT '订单ID，雪花算法生成',
  `order_no` varchar(32) NOT NULL COMMENT '订单编号，唯一',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `product_id` bigint(20) NOT NULL COMMENT '商品ID',
  `product_name` varchar(128) NOT NULL COMMENT '商品名称快照',
  `quantity` int(11) NOT NULL COMMENT '购买数量',
  `unit_price` decimal(12,2) NOT NULL COMMENT '商品单价快照',
  `total_amount` decimal(12,2) NOT NULL COMMENT '订单总金额',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `ship_time` datetime DEFAULT NULL COMMENT '发货时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `address` varchar(255) NOT NULL COMMENT '收货地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  -- 复合索引：用户按状态查订单
  KEY `idx_user_status` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =====================================
-- 订单库：undo_log Seata AT模式所需
-- =====================================
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
  `branch_id` bigint(20) NOT NULL COMMENT '分支事务ID',
  `xid` varchar(128) NOT NULL COMMENT '全局事务ID',
  `context` varchar(128) NOT NULL COMMENT '上下文',
  `rollback_info` longblob NOT NULL COMMENT '回滚信息',
  `log_status` int(11) NOT NULL COMMENT '状态：0-正常，1-全局已完成',
  `log_created` datetime NOT NULL COMMENT '创建时间',
  `log_modified` datetime NOT NULL COMMENT '修改时间',
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata回滚日志表';
