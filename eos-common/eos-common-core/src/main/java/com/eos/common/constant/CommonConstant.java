package com.eos.common.constant;

/**
 * 通用常量定义
 *
 * <p>将项目中频繁使用的字符串、数字常量集中管理，避免"魔法值"散落在代码中。
 * 这是代码规范（如阿里巴巴Java开发手册）的强制要求。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
public final class CommonConstant {

    private CommonConstant() {
        // 工具类禁止实例化
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    // ==================== 系统通用 ====================

    /** 请求成功标识 */
    public static final String SUCCESS = "success";

    /** 请求失败标识 */
    public static final String FAIL = "fail";

    /** 是 */
    public static final Integer YES = 1;

    /** 否 */
    public static final Integer NO = 0;

    /** 管理员角色 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 普通用户角色 */
    public static final String ROLE_USER = "USER";

    // ==================== HTTP Header ====================

    /** 请求ID，用于链路追踪 */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** 用户Token */
    public static final String HEADER_TOKEN = "Authorization";

    /** Token前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 用户ID */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 用户名 */
    public static final String HEADER_USERNAME = "X-Username";

    /** 用户角色 */
    public static final String HEADER_ROLE = "X-Role";

    /** 时间戳 */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    /** 签名 */
    public static final String HEADER_SIGN = "X-Sign";

    // ==================== 缓存Key前缀 ====================

    /** 用户缓存前缀 */
    public static final String CACHE_USER = "user:";

    /** 商品缓存前缀 */
    public static final String CACHE_PRODUCT = "product:";

    /** 订单缓存前缀 */
    public static final String CACHE_ORDER = "order:";

    /** 库存缓存前缀 */
    public static final String CACHE_STOCK = "stock:";

    /** Token黑名单缓存前缀 */
    public static final String CACHE_TOKEN_BLACKLIST = "token:blacklist:";

    /** 会话缓存前缀 */
    public static final String CACHE_SESSION = "session:";

    /** 分布式锁前缀 */
    public static final String LOCK_PREFIX = "lock:";

    /** 幂等性前缀 */
    public static final String IDEMPOTENT_PREFIX = "idempotent:";

    // ==================== 时间常量（单位：秒） ====================

    /** 1分钟 */
    public static final long ONE_MINUTE = 60L;

    /** 5分钟 */
    public static final long FIVE_MINUTE = 300L;

    /** 15分钟 */
    public static final long FIFTEEN_MINUTE = 900L;

    /** 30分钟 */
    public static final long THIRTY_MINUTE = 1800L;

    /** 1小时 */
    public static final long ONE_HOUR = 3600L;

    /** 1天 */
    public static final long ONE_DAY = 86400L;

    /** 7天 */
    public static final long SEVEN_DAY = 604800L;

    // ==================== 分页默认值 ====================

    /** 默认页码 */
    public static final long DEFAULT_PAGE_NO = 1L;

    /** 默认每页大小 */
    public static final long DEFAULT_PAGE_SIZE = 10L;

    /** 最大每页大小 */
    public static final long MAX_PAGE_SIZE = 100L;
}
