package com.eos.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图对象
 *
 * <p>VO（View Object）用于封装返回给前端的数据，通常会对敏感字段进行脱敏处理。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 手机号（脱敏） */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 昵称 */
    private String nickname;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 角色 */
    private String role;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
