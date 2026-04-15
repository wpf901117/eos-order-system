package com.eos.order.domain.valueobject;

import lombok.Getter;

import java.io.Serializable;

/**
 * 收货地址值对象
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Getter
public class Address implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 省份 */
    private final String province;

    /** 城市 */
    private final String city;

    /** 区县 */
    private final String district;

    /** 详细地址 */
    private final String detailAddress;

    /** 收件人 */
    private final String receiver;

    /** 联系电话 */
    private final String phone;

    public Address(String province, String city, String district, 
                   String detailAddress, String receiver, String phone) {
        this.province = province;
        this.city = city;
        this.district = district;
        this.detailAddress = detailAddress;
        this.receiver = receiver;
        this.phone = phone;
    }

    /**
     * 获取完整地址
     */
    public String getFullAddress() {
        return String.format("%s%s%s%s", province, city, district, detailAddress);
    }

    /**
     * 验证地址是否有效
     */
    public boolean isValid() {
        return province != null && !province.isEmpty()
            && city != null && !city.isEmpty()
            && detailAddress != null && !detailAddress.isEmpty()
            && receiver != null && !receiver.isEmpty()
            && phone != null && !phone.isEmpty();
    }

    @Override
    public String toString() {
        return getFullAddress() + " (" + receiver + " " + phone + ")";
    }
}
