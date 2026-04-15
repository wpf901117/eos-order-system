package com.eos.order.domain.valueobject;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 金额值对象
 *
 * <p>不可变对象，保证金额的安全性和一致性。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public class Money implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 金额 */
    private final BigDecimal amount;

    /** 货币单位 */
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
        this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currency = currency != null ? currency : "CNY";
    }

    /**
     * 创建金额对象
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount, "CNY");
    }

    /**
     * 创建金额对象（指定货币）
     */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    /**
     * 加法
     */
    public Money add(Money other) {
        checkCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 减法
     */
    public Money subtract(Money other) {
        checkCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * 乘法
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(new BigDecimal(multiplier)), this.currency);
    }

    /**
     * 是否为零
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 是否大于另一个金额
     */
    public boolean isGreaterThan(Money other) {
        checkCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * 检查货币单位是否一致
     */
    private void checkCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("货币单位不一致");
        }
    }

    @Override
    public String toString() {
        return currency + " " + amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return amount.hashCode() * 31 + currency.hashCode();
    }
}
