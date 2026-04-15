package com.eos.order.service;

import com.eos.common.result.PageResult;
import com.eos.order.dto.OrderCreateDTO;
import com.eos.order.vo.OrderVO;

/**
 * 订单服务接口
 *
 * @author EOS Team
 * @since 1.0.0
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param userId 当前登录用户ID
     * @param dto 订单创建信息
     * @return 订单信息
     */
    OrderVO createOrder(Long userId, OrderCreateDTO dto);

    /**
     * 根据ID查询订单
     *
     * @param orderId 订单ID
     * @param userId 当前登录用户ID
     * @param role 当前用户角色
     * @return 订单信息
     */
    OrderVO getOrderById(Long orderId, Long userId, String role);

    /**
     * 查询当前用户订单列表
     *
     * @param userId 当前登录用户ID
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @param status 订单状态
     * @param orderNo 订单编号
     * @return 分页订单列表
     */
    PageResult<OrderVO> listCurrentUserOrders(Long userId, Long pageNo, Long pageSize, Integer status, String orderNo);

    /**
     * 查询订单列表（管理员）
     *
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @param status 订单状态
     * @param orderNo 订单编号
     * @param userId 用户ID筛选
     * @return 分页订单列表
     */
    PageResult<OrderVO> listOrders(Long pageNo, Long pageSize, Integer status, String orderNo, Long userId, String role);

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @param userId 当前登录用户ID
     * @param role 当前用户角色
     * @return 订单信息
     */
    OrderVO payOrder(Long orderId, Long userId, String role);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @param userId 当前登录用户ID
     * @param role 当前用户角色
     */
    void cancelOrder(Long orderId, Long userId, String role);

    /**
     * 管理员发货
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
    OrderVO shipOrder(Long orderId, String role);

    /**
     * 用户确认收货
     *
     * @param orderId 订单ID
     * @param userId 当前登录用户ID
     * @param role 当前用户角色
     * @return 订单信息
     */
    OrderVO confirmOrder(Long orderId, Long userId, String role);

    /**
     * 超时取消订单
     *
     * @param orderId 订单ID
     */
    void timeoutCancelOrder(Long orderId);
}
