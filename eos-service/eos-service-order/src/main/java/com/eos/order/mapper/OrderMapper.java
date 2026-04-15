package com.eos.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eos.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单Mapper接口
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单对象
     */
    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo} AND deleted = 0 LIMIT 1")
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询用户订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    @Select("SELECT * FROM t_order WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserId(@Param("userId") Long userId);
}
