package com.eos.order.controller;

import com.eos.common.annotation.Idempotent;
import com.eos.common.annotation.RateLimit;
import com.eos.common.constant.CommonConstant;
import com.eos.common.exception.BizException;
import com.eos.common.result.PageResult;
import com.eos.common.result.Result;
import com.eos.common.result.ResultCode;
import com.eos.order.dto.OrderCreateDTO;
import com.eos.order.service.OrderService;
import com.eos.order.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 *
 * @author EOS Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * 
     * <p>高并发防护：</p>
     * <ul>
     *   <li>幂等性：防止用户重复提交（30秒内同一用户+商品只能提交一次）</li>
     *   <li>限流：单用户每分钟最多创建10个订单</li>
     * </ul>
     */
    @PostMapping("/create")
    @Idempotent(key = "#userId + ':' + #dto.productId", expire = 30, message = "请勿重复提交订单")
    @RateLimit(key = "#userId", maxRequests = 10, windowSeconds = 60, message = "下单过于频繁，请稍后再试")
    public Result<OrderVO> createOrder(@RequestHeader(value = CommonConstant.HEADER_USER_ID, required = false) Long userId,
                                       @RequestBody @Validated OrderCreateDTO dto) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        OrderVO orderVO = orderService.createOrder(userId, dto);
        return Result.ok(orderVO);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderId}")
    public Result<OrderVO> getOrderById(@PathVariable Long orderId,
                                        @RequestHeader(value = CommonConstant.HEADER_USER_ID, required = false) Long userId,
                                        @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return Result.ok(orderService.getOrderById(orderId, userId, role));
    }

    /**
     * 查询当前用户订单列表
     */
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> listCurrentUserOrders(
            @RequestHeader(value = CommonConstant.HEADER_USER_ID, required = false) Long userId,
            @RequestParam(defaultValue = "1") Long pageNo,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return Result.ok(orderService.listCurrentUserOrders(userId, pageNo, pageSize, status, orderNo));
    }

    /**
     * 查询订单列表（管理员）
     */
    @GetMapping("/admin/list")
    public Result<PageResult<OrderVO>> listOrders(
            @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role,
            @RequestParam(defaultValue = "1") Long pageNo,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId) {
        return Result.ok(orderService.listOrders(pageNo, pageSize, status, orderNo, userId, role));
    }

    /**
     * 支付订单
     * 
     * <p>幂等性：防止重复支付（60秒内同一订单只能支付一次）</p>
     */
    @PostMapping("/{orderId}/pay")
    @Idempotent(key = "#orderId", expire = 60, message = "请勿重复支付")
    public Result<OrderVO> payOrder(@PathVariable Long orderId,
                                    @RequestHeader(value = CommonConstant.HEADER_USER_ID, required = false) Long userId,
                                    @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return Result.ok(orderService.payOrder(orderId, userId, role));
    }

    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long orderId,
                                    @RequestHeader(value = CommonConstant.HEADER_USER_ID, required = false) Long userId,
                                    @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        orderService.cancelOrder(orderId, userId, role);
        return Result.ok();
    }

    /**
     * 管理员发货
     */
    @PostMapping("/{orderId}/ship")
    public Result<OrderVO> shipOrder(@PathVariable Long orderId,
                                      @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role) {
        return Result.ok(orderService.shipOrder(orderId, role));
    }

    /**
     * 确认收货
     */
    @PostMapping("/{orderId}/confirm")
    public Result<OrderVO> confirmOrder(@PathVariable Long orderId,
                                        @RequestHeader(value = CommonConstant.HEADER_USER_ID, required = false) Long userId,
                                        @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return Result.ok(orderService.confirmOrder(orderId, userId, role));
    }
}
