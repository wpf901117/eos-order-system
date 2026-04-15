package com.eos.order.domain.statemachine;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单状态机
 *
 * <p>管理订单状态的合法流转，防止非法状态变更。</p>
 *
 * <p><strong>状态流转图：</strong></p>
 * <pre>
 * 待支付(0) → 已支付(1) → 已发货(2) → 已完成(3)
 *     ↓
 *  已取消(4)
 * </pre>
 *
 * @author EOS Team
 * @since 1.0.0
 */
public class OrderStateMachine {

    /** 状态转换表：当前状态 -> 可转换的下一个状态 */
    private static final Map<Integer, int[]> STATE_TRANSITIONS = new HashMap<>();

    static {
        // 待支付 → 已支付、已取消
        STATE_TRANSITIONS.put(0, new int[]{1, 4});
        
        // 已支付 → 已发货
        STATE_TRANSITIONS.put(1, new int[]{2});
        
        // 已发货 → 已完成
        STATE_TRANSITIONS.put(2, new int[]{3});
        
        // 已完成 → 无
        STATE_TRANSITIONS.put(3, new int[]{});
        
        // 已取消 → 无
        STATE_TRANSITIONS.put(4, new int[]{});
    }

    /**
     * 检查状态转换是否合法
     *
     * @param currentState 当前状态
     * @param nextState 下一个状态
     * @return 是否合法
     */
    public static boolean canTransition(int currentState, int nextState) {
        int[] allowedStates = STATE_TRANSITIONS.get(currentState);
        if (allowedStates == null) {
            return false;
        }
        
        for (int state : allowedStates) {
            if (state == nextState) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 执行状态转换（带校验）
     *
     * @param currentState 当前状态
     * @param nextState 下一个状态
     * @throws IllegalStateException 如果状态转换不合法
     */
    public static void transition(int currentState, int nextState) {
        if (!canTransition(currentState, nextState)) {
            throw new IllegalStateException(
                String.format("非法的状态转换: %d -> %d", currentState, nextState)
            );
        }
    }

    /**
     * 获取状态描述
     */
    @Getter
    public enum OrderStatus {
        PENDING_PAYMENT(0, "待支付"),
        PAID(1, "已支付"),
        SHIPPED(2, "已发货"),
        COMPLETED(3, "已完成"),
        CANCELLED(4, "已取消");

        private final int code;
        private final String description;

        OrderStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public static OrderStatus fromCode(int code) {
            for (OrderStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("未知的订单状态: " + code);
        }
    }
}
