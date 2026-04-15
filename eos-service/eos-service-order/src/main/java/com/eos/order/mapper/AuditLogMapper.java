package com.eos.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eos.order.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
