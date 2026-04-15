package com.eos.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一API响应封装类
 *
 * <p>这是构建RESTful API的核心基础类，体现了"统一接口规范"的工程化思想。</p>
 *
 * <p><strong>为什么需要统一响应？</strong></p>
 * <ul>
 *   <li>前端对接统一：无论调用哪个接口，响应结构完全一致</li>
 *   <li>异常处理统一：错误码和错误信息标准化</li>
 *   <li>日志和监控统一：可以统一拦截处理</li>
 * </ul>
 *
 * <p><strong>设计原则：</strong></p>
 * <ul>
 *   <li>泛型设计：{@code Result<T>} 支持任意类型的数据返回</li>
 *   <li>不可变性：建议返回后不再修改（生产环境可用record或Builder模式）</li>
 *   <li>链式调用：支持流式代码编写</li>
 * </ul>
 *
 * @param <T> 响应数据类型
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     * <p>200 表示成功，其他表示各种业务/系统错误</p>
     */
    private Integer code;

    /**
     * 响应消息
     * <p>成功时通常为"success"，失败时为具体的错误描述</p>
     */
    private String message;

    /**
     * 响应数据
     * <p>业务成功时返回的具体数据，失败时可能为null</p>
     */
    private T data;

    /**
     * 时间戳
     * <p>方便问题排查时定位请求时间</p>
     */
    private Long timestamp;

    /**
     * 私有构造方法，强制使用静态工厂方法创建实例
     * 这是《Effective Java》第1条推荐的做法
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造成功的响应结果
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 构造无数据的成功响应
     *
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 构造失败的响应结果
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 基于预定义错误码构造失败响应
     *
     * @param resultCode 结果码枚举
     * @param <T>        数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return error(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 基于预定义错误码构造失败响应，并覆盖错误消息
     *
     * @param resultCode 结果码枚举
     * @param message    自定义错误消息
     * @param <T>        数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return error(resultCode.getCode(), message);
    }

    /**
     * 判断是否成功
     *
     * @return true 如果code等于200
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
