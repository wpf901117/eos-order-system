package com.eos.common.exception;

import com.eos.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * <p>业务异常与系统异常的核心区别：</p>
 * <ul>
 *   <li><strong>业务异常</strong>：可预期的业务流程中断，如库存不足、订单超时等。
 *       这类异常不应该打印ERROR级别日志（避免告警轰炸），返回给前端明确的错误信息。</li>
 *   <li><strong>系统异常</strong>：不可预期的程序错误，如NPE、数据库连接失败等。
 *       必须打印ERROR日志并触发告警，返回给前端模糊的"系统繁忙"提示。</li>
 * </ul>
 *
 * <p><strong>最佳实践：</strong></p>
 * <ul>
 *   <li>Service层抛出业务异常，Controller层统一捕获</li>
 *   <li>业务异常使用WARN级别日志记录</li>
 *   <li>错误信息可直接展示给用户</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final Integer code;

    /** 错误消息 */
    private final String message;

    /**
     * 基于ResultCode构造业务异常
     *
     * @param resultCode 预定义的结果码
     */
    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 基于ResultCode构造业务异常，可覆盖错误消息
     *
     * @param resultCode 预定义的结果码
     * @param message    自定义错误消息
     */
    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    /**
     * 自定义错误码和消息
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 携带原始异常的业务异常
     *
     * @param resultCode 预定义的结果码
     * @param cause      原始异常
     */
    public BizException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }
}
