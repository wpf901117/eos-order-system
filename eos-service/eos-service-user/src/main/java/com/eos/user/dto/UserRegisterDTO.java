package com.eos.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户注册DTO
 *
 * <p>DTO（Data Transfer Object）用于层与层之间的数据传输。
 * 与Entity不同，DTO只包含当前业务场景需要的字段，并附带校验注解。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class UserRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20位之间")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱（可选） */
    private String email;

    /** 昵称（可选） */
    private String nickname;
}
