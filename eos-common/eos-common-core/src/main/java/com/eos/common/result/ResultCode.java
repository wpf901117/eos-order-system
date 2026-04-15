package com.eos.common.result;

import lombok.Getter;

/**
 * 统一响应码枚举
 *
 * <p>遵循HTTP状态码规范进行扩展设计：</p>
 * <ul>
 *   <li>1xx：信息，服务器收到请求，需要请求者继续执行操作</li>
 *   <li>2xx：成功，操作被成功接收并处理</li>
 *   <li>3xx：重定向，需要进一步的操作以完成请求</li>
 *   <li>4xx：客户端错误，请求包含语法错误或无法完成请求</li>
 *   <li>5xx：服务器错误，服务器在处理请求的过程中发生了错误</li>
 * </ul>
 *
 * <p><strong>本项目错误码规划：</strong></p>
 * <pre>
 * 200xx - 通用成功
 * 400xx - 客户端参数错误
 * 401xx - 认证授权错误
 * 403xx - 权限不足
 * 404xx - 资源不存在
 * 429xx - 限流熔断
 * 500xx - 服务端系统错误
 * 600xx - 业务逻辑错误
 * </pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public enum ResultCode {

    // ==================== 成功 (2xx) ====================
    /** 操作成功 */
    SUCCESS(200, "success"),

    // ==================== 客户端错误 (4xx) ====================
    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 参数校验失败 */
    PARAM_VALID_ERROR(40001, "参数校验失败"),
    /** 请求体格式错误 */
    PARAM_FORMAT_ERROR(40002, "请求体格式错误"),

    /** 未登录或Token已过期 */
    UNAUTHORIZED(401, "未登录或登录已过期"),
    /** Token无效 */
    TOKEN_INVALID(40101, "Token无效或已过期"),
    /** Token已被登出 */
    TOKEN_LOGOUT(40102, "Token已被登出"),

    /** 权限不足 */
    FORBIDDEN(403, "权限不足，拒绝访问"),
    /** 缺少必要权限 */
    PERMISSION_DENIED(40301, "缺少访问权限"),
    /** 用户无权访问该订单 */
    ORDER_ACCESS_DENIED(40302, "无权访问该订单"),

    /** 资源不存在 */
    NOT_FOUND(404, "请求的资源不存在"),
    /** 用户不存在 */
    USER_NOT_FOUND(40401, "用户不存在"),
    /** 订单不存在 */
    ORDER_NOT_FOUND(40402, "订单不存在"),
    /** 商品不存在 */
    PRODUCT_NOT_FOUND(40403, "商品不存在"),

    /** 请求过于频繁 */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    /** 重复请求 */
    DUPLICATE_REQUEST(42901, "请勿重复提交"),

    // ==================== 服务端错误 (5xx) ====================
    /** 系统内部错误 */
    INTERNAL_SERVER_ERROR(500, "系统内部错误，请稍后再试"),
    /** 数据库操作失败 */
    DB_ERROR(50001, "数据库操作失败"),
    /** 远程服务调用失败 */
    RPC_ERROR(50002, "远程服务调用失败"),
    /** 缓存服务异常 */
    CACHE_ERROR(50003, "缓存服务异常"),
    /** 消息队列异常 */
    MQ_ERROR(50004, "消息队列异常"),

    // ==================== 业务错误 (6xx) ====================
    /** 业务操作失败 */
    BIZ_ERROR(600, "业务操作失败"),
    /** 用户名已存在 */
    USERNAME_EXISTS(60001, "用户名已存在"),
    /** 手机号已注册 */
    PHONE_EXISTS(60002, "手机号已注册"),
    /** 用户名或密码错误 */
    LOGIN_ERROR(60003, "用户名或密码错误"),
    /** 账户已被锁定 */
    ACCOUNT_LOCKED(60004, "账户已被锁定"),

    /** 库存不足 */
    STOCK_NOT_ENOUGH(60101, "商品库存不足"),
    /** 商品已下架 */
    PRODUCT_OFF_SHELF(60102, "商品已下架"),

    /** 订单状态异常 */
    ORDER_STATUS_ERROR(60201, "订单状态异常，无法执行该操作"),
    /** 订单已超时 */
    ORDER_TIMEOUT(60202, "订单已超时，请重新下单"),
    /** 重复提交订单 */
    ORDER_REPEAT_SUBMIT(60203, "请勿重复提交订单"),

    /** 支付金额不匹配 */
    PAY_AMOUNT_ERROR(60301, "支付金额不匹配"),
    /** 支付状态异常 */
    PAY_STATUS_ERROR(60302, "支付状态异常"),

    /** 分布式锁获取失败 */
    DISTRIBUTED_LOCK_ERROR(60401, "系统繁忙，请稍后再试");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
