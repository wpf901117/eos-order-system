package com.eos.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Token视图对象
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class TokenVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** Token类型 */
    private String tokenType;

    /** 访问令牌有效期（秒） */
    private Long expiresIn;

    public TokenVO() {
        this.tokenType = "Bearer";
    }

    public TokenVO(String accessToken, String refreshToken, Long expiresIn) {
        this();
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }
}
