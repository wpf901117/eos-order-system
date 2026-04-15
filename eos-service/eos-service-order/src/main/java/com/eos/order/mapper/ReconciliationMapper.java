package com.eos.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eos.order.entity.Reconciliation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账记录 Mapper
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Mapper
public interface ReconciliationMapper extends BaseMapper<Reconciliation> {
}
