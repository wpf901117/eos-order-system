package com.eos.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eos.order.entity.ReconciliationDiff;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账差异明细 Mapper
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Mapper
public interface ReconciliationDiffMapper extends BaseMapper<ReconciliationDiff> {
}
