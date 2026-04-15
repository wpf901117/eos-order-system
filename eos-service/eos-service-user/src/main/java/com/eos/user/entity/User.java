package com.eos.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * <p>对应数据库表 <code>t_user</code>，采用 MyBatis Plus 注解进行 ORM 映射。</p>
 *
 * <p><strong>注解说明：</strong></p>
 * <ul>
 *   <li>{@link TableName} - 指定表名</li>
 *   <li>{@link TableId} - 主键字段，<code>type = IdType.ASSIGN_ID</code> 使用雪花算法</li>
 *   <li>{@link TableField} - 普通字段映射，<code>fill</code> 指定自动填充策略</li>
 *   <li>{@link TableLogic} - 逻辑删除字段</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Data
@TableName("t_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID，主键，雪花算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户名，唯一索引 */
    private String username;

    /** 密码，BCrypt加密存储 */
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 昵称 */
    private String nickname;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 角色：ADMIN-管理员，USER-普通用户 */
    private String role;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除字段：0-未删除，1-已删除 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
