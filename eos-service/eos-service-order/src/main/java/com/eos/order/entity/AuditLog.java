package com.eos.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志实体
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
@TableName("t_audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 模块名称 */
    private String module;

    /** 操作描述 */
    private String operation;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 请求IP */
    private String ipAddress;

    /** 请求方法 */
    private String method;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private String requestParams;

    /** 响应结果 */
    private String responseResult;

    /** 执行时长（毫秒） */
    private Long duration;

    /** 操作状态：0-失败，1-成功 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;
}
