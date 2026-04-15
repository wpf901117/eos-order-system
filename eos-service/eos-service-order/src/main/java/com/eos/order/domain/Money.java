package com.eos.order.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 金额值对象（DDD Value Object）
 *
 * <p>值对象的特性：</p>
 * <ul>
 *   <li>不可变性（Immutable）</li>
 *   <li>通过值相等性判断（而非引用）</li>
 *   <li>无身份标识</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
public class Money {

    /** 金额 */
    private final BigDecimal amount;

    /** 货币单位（默认人民币） */
    private final String currency;

    /** 零金额常量 */
    public static final Money ZERO = new Money(BigDecimal.ZERO, "CNY");

    /**
     * 构造函数
     *
     * @param amount 金额
     * @param currency 货币单位
     */
    public Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("金额不能为null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
        this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currency = currency != null ? currency : "CNY";
    }

    /**
     * 简化构造函数（默认人民币）
     *
     * @param amount 金额
     */
    public Money(BigDecimal amount) {
        this(amount, "CNY");
    }

    /**
     * 加法运算
     *
     * @param other 另一个金额
     * @return 新的金额对象
     */
    public Money add(Money other) {
        checkCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 减法运算
     *
     * @param other 另一个金额
     * @return 新的金额对象
     */
    public Money subtract(Money other) {
        checkCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new ArithmeticException("结果不能为负数");
        }
        return new Money(result, this.currency);
    }

    /**
     * 乘法运算
     *
     * @param multiplier 乘数
     * @return 新的金额对象
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(new BigDecimal(multiplier)), this.currency);
    }

    /**
     * 乘法运算（BigDecimal）
     *
     * @param multiplier 乘数
     * @return 新的金额对象
     */
    public Money multiply(BigDecimal multiplier) {
        if (multiplier == null) {
            throw new IllegalArgumentException("乘数不能为null");
        }
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * 比较大小
     *
     * @param other 另一个金额
     * @return -1、0、1
     */
    public int compareTo(Money other) {
        checkCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    /**
     * 判断是否大于
     *
     * @param other 另一个金额
     * @return true 如果大于
     */
    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    /**
     * 判断是否小于
     *
     * @param other 另一个金额
     * @return true 如果小于
     */
    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    /**
     * 检查货币单位是否一致
     *
     * @param other 另一个金额
     */
    private void checkCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "货币单位不一致: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
