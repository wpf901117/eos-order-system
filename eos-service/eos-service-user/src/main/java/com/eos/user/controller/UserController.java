package com.eos.user.controller;

import com.eos.common.result.Result;
import com.eos.common.util.JwtUtil;
import com.eos.user.dto.UserLoginDTO;
import com.eos.user.dto.UserRegisterDTO;
import com.eos.user.service.UserService;
import com.eos.user.vo.TokenVO;
import com.eos.user.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * <p>RESTful API 设计规范：</p>
 * <ul>
 *   <li>GET - 获取资源</li>
 *   <li>POST - 创建资源</li>
 *   <li>PUT - 更新资源</li>
 *   <li>DELETE - 删除资源</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     *
     * @param dto 注册信息（@Validated 触发参数校验）
     * @return 用户信息
     */
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Validated UserRegisterDTO dto) {
        UserVO userVO = userService.register(dto);
        return Result.ok(userVO);
    }

    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return Token信息
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@RequestBody @Validated UserLoginDTO dto) {
        TokenVO tokenVO = userService.login(dto);
        return Result.ok(tokenVO);
    }

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        UserVO userVO = userService.getUserById(userId);
        return Result.ok(userVO);
    }

    /**
     * 用户登出
     *
     * @param authorization Authorization请求头
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = JwtUtil.extractToken(authorization);
        userService.logout(token);
        return Result.ok();
    }

    /**
     * 刷新访问令牌
     *
     * <p>使用refresh token换取新的access token，实现滑动过期保活。
     * 这是公开接口，网关白名单放行。</p>
     *
     * @param refreshToken 刷新令牌
     * @return 新的Token信息
     */
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@RequestBody java.util.Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        TokenVO tokenVO = userService.refresh(refreshToken);
        return Result.ok(tokenVO);
    }
}
