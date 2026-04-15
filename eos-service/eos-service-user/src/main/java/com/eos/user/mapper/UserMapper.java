package com.eos.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eos.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper接口
 *
 * <p>继承 {@link BaseMapper} 即可获得基础的CRUD方法，无需手写XML。</p>
 *
 * <p>对于复杂查询，可以在 resources/mapper 目录下编写对应的 XML 文件，
 * 或者在接口方法上直接使用 {@link Select} 等注解。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户对象
     */
    @Select("SELECT * FROM t_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户对象
     */
    @Select("SELECT * FROM t_user WHERE phone = #{phone} AND deleted = 0 LIMIT 1")
    User selectByPhone(@Param("phone") String phone);
}
