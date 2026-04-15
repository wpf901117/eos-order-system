package com.eos.user.service;

import com.eos.user.dto.UserLoginDTO;
import com.eos.user.dto.UserRegisterDTO;
import com.eos.user.vo.TokenVO;
import com.eos.user.vo.UserVO;

/**
 * 用户服务接口
 *
 * <p>接口定义遵循单一职责原则，每个方法对应一个明确的业务能力。</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param dto 注册信息
     * @return 用户视图对象
     */
    UserVO register(UserRegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return Token信息
     */
    TokenVO login(UserLoginDTO dto);

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户视图对象
     */
    UserVO getUserById(Long userId);

    /**
     * 用户登出
     *
     * @param token 访问令牌
     */
    void logout(String token);

    /**
     * 刷新访问令牌（滑动过期）
     *
     * @param refreshToken 刷新令牌
     * @return 新的Token信息
     */
    TokenVO refresh(String refreshToken);
}
