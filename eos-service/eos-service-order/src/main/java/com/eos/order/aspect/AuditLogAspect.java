package com.eos.order.aspect;

import com.eos.order.annotation.AuditLog;
import com.eos.order.entity.AuditLog;
import com.eos.order.mapper.AuditLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 审计日志切面
 *
 * <p>拦截带有 @AuditLog 注解的方法，自动记录审计日志。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    @Autowired
    private AuditLogMapper auditLogMapper;

    /**
     * 环绕通知：记录审计日志
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     */
    @Around("@annotation(com.eos.order.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLogAnnotation = method.getAnnotation(AuditLog.class);

        // 创建审计日志对象
        AuditLog auditLog = new AuditLog();
        auditLog.setModule(auditLogAnnotation.module());
        auditLog.setOperation(auditLogAnnotation.operation());
        auditLog.setCreateTime(LocalDateTime.now());

        try {
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getIpAddress(request));
                auditLog.setRequestUrl(request.getRequestURI());
                auditLog.setMethod(request.getMethod());
            }

            // 记录请求参数
            if (auditLogAnnotation.recordParams()) {
                auditLog.setRequestParams(getRequestParams(joinPoint));
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录响应结果
            if (auditLogAnnotation.recordResult() && result != null) {
                auditLog.setResponseResult(result.toString());
            }

            // 记录成功状态
            auditLog.setStatus(1);
            auditLog.setDuration(System.currentTimeMillis() - startTime);

            return result;

        } catch (Throwable e) {
            // 记录失败状态
            auditLog.setStatus(0);
            auditLog.setErrorMsg(e.getMessage());
            auditLog.setDuration(System.currentTimeMillis() - startTime);

            throw e;

        } finally {
            // 异步保存审计日志（避免影响主流程性能）
            saveAuditLogAsync(auditLog);
        }
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return "";
        }

        StringBuilder params = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                params.append(arg.toString()).append(", ");
            }
        }

        if (params.length() > 0) {
            params.setLength(params.length() - 2);
        }

        return params.toString();
    }

    /**
     * 异步保存审计日志
     *
     * @param auditLog 审计日志
     */
    private void saveAuditLogAsync(AuditLog auditLog) {
        // TODO: 使用线程池或消息队列异步保存
        // 这里简化为同步保存，生产环境应改为异步
        try {
            auditLogMapper.insert(auditLog);
            log.debug("[审计日志] 保存成功: module={}, operation={}", 
                     auditLog.getModule(), auditLog.getOperation());
        } catch (Exception e) {
            log.error("[审计日志] 保存失败", e);
        }
    }
}
