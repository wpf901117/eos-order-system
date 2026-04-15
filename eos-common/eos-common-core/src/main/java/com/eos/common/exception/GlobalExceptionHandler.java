package com.eos.common.exception;

import com.eos.common.result.Result;
import com.eos.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>这是Spring Boot中统一异常处理的核心机制，通过{@code @RestControllerAdvice}实现AOP切面拦截。
 * 它的价值在于：</p>
 * <ul>
 *   <li>Controller层不再需要try-catch，代码更简洁</li>
 *   <li>所有异常统一转换为标准{@link Result}响应</li>
 *   <li>可以统一记录日志、发送告警</li>
 * </ul>
 *
 * <p><strong>异常处理优先级：</strong></p>
 * <ol>
 *   <li>业务异常 {@link BizException} - 预期内的错误</li>
 *   <li>参数校验异常 - 用户输入不合法</li>
 *   <li>其他所有异常 - 系统Bug或外部依赖故障</li>
 * </ol>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * <p>业务异常不需要打印ERROR堆栈，避免污染日志和触发不必要的告警。
     * 使用WARN级别记录即可，因为这类异常是系统可预期和控制的。</p>
     *
     * @param e 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("[业务异常] code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常 - @Valid在Controller方法参数上
     *
     * <p>例如：@Valid @RequestBody UserDTO dto</p>
     *
     * @param e 方法参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[参数校验失败] {}", message);
        return Result.error(ResultCode.PARAM_VALID_ERROR, message);
    }

    /**
     * 处理参数绑定异常 - 表单提交或URL参数绑定失败
     *
     * @param e 参数绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[参数绑定失败] {}", message);
        return Result.error(ResultCode.PARAM_VALID_ERROR, message);
    }

    /**
     * 处理单个参数校验异常 - @RequestParam @NotNull等
     *
     * @param e 约束违反异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("[参数校验失败] {}", message);
        return Result.error(ResultCode.PARAM_VALID_ERROR, message);
    }

    /**
     * 兜底异常处理器
     *
     * <p>所有未被前面处理器捕获的异常都会到这里。
     * 这类异常通常是系统Bug或外部依赖故障，必须：</p>
     * <ul>
     *   <li>打印完整ERROR日志和堆栈</li>
     *   <li>发送告警通知（生产环境）</li>
     *   <li>返回模糊的"系统繁忙"提示（避免暴露内部信息）</li>
     * </ul>
     *
     * @param e 未知异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("[系统异常] ", e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
