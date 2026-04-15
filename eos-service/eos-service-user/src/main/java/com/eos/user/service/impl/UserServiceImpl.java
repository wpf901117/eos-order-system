package com.eos.user.service.impl;

import com.eos.common.constant.CommonConstant;
import com.eos.common.exception.BizException;
import com.eos.common.result.ResultCode;
import com.eos.common.util.JwtUtil;
import com.eos.common.util.SnowflakeUtil;
import com.eos.user.dto.UserLoginDTO;
import com.eos.user.dto.UserRegisterDTO;
import com.eos.user.entity.User;
import com.eos.user.mapper.UserMapper;
import com.eos.user.service.UserService;
import com.eos.user.vo.TokenVO;
import com.eos.user.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 *
 * <p>本类展示了多个十年经验级别的技术点：</p>
 * <ul>
 *   <li><strong>事务控制</strong>：@Transactional 保证注册流程的原子性</li>
 *   <li><strong>密码安全</strong>：BCrypt 单向哈希加密，自带 salt 防彩虹表</li>
 *   <li><strong>并发安全</strong>：用户名/手机号唯一性校验</li>
 *   <li><strong>缓存策略</strong>：JWT + Redis 黑名单实现有状态的登出</li>
 *   <li><strong>分布式ID</strong>：雪花算法生成用户ID</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /** AccessToken有效期：30分钟（秒） */
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 30 * 60;

    /** RefreshToken有效期：7天（秒） */
    private static final long REFRESH_TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    /** Session有效期：7天（秒） */
    private static final long SESSION_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    /**
     * 用户注册
     *
     * <p>注册流程：</p>
     * <ol>
     *   <li>校验用户名和手机号唯一性</li>
     *   <li>BCrypt加密密码</li>
     *   <li>雪花算法生成用户ID</li>
     *   <li>保存用户到数据库</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(UserRegisterDTO dto) {
        // 1. 校验用户名是否已存在
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }

        // 2. 校验手机号是否已注册
        if (userMapper.selectByPhone(dto.getPhone()) != null) {
            throw new BizException(ResultCode.PHONE_EXISTS);
        }

        // 3. 构建用户实体
        User user = new User();
        user.setId(SnowflakeUtil.getInstance().nextId());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(maskPhone(dto.getPhone()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setStatus(1);
        user.setRole("USER");

        // 4. 保存到数据库
        userMapper.insert(user);

        log.info("[用户注册] 用户 {} 注册成功，userId={}", user.getUsername(), user.getId());

        return convertToVO(user);
    }

    /**
     * 用户登录
     *
     * <p>登录流程：</p>
     * <ol>
     *   <li>根据用户名查询用户</li>
     *   <li>校验密码</li>
     *   <li>校验账户状态</li>
     *   <li>生成JWT Token（含sessionId）</li>
     *   <li>持久化Session到Redis</li>
     *   <li>更新最后登录时间</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenVO login(UserLoginDTO dto) {
        // 1. 查询用户
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BizException(ResultCode.LOGIN_ERROR);
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("[登录失败] 用户名 {} 密码错误", dto.getUsername());
            throw new BizException(ResultCode.LOGIN_ERROR);
        }

        // 3. 校验账户状态
        if (user.getStatus() == 0) {
            throw new BizException(ResultCode.ACCOUNT_LOCKED);
        }

        // 4. 生成会话ID和Token
        String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());

        String accessToken = JwtUtil.generateAccessToken(claims, sessionId);
        String refreshToken = JwtUtil.generateRefreshToken(claims, sessionId);

        // 5. 持久化Session到Redis
        String sessionKey = CommonConstant.CACHE_SESSION + sessionId;
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        hashOps.put(sessionKey, "userId", String.valueOf(user.getId()));
        hashOps.put(sessionKey, "username", user.getUsername());
        hashOps.put(sessionKey, "role", user.getRole());
        hashOps.put(sessionKey, "refreshToken", refreshToken);
        hashOps.put(sessionKey, "lastAccessTime", String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(sessionKey, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 6. 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("[用户登录] 用户 {} 登录成功，sessionId={}", user.getUsername(), sessionId);

        return new TokenVO(accessToken, refreshToken, ACCESS_TOKEN_EXPIRE_SECONDS);
    }

    /**
     * 根据ID查询用户
     */
    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    /**
     * 用户登出
     *
     * <p>基于Session撤销的登出：删除Redis中的Session记录，
     * 网关通过检查Session是否存在来决定Token是否有效。</p>
     */
    @Override
    public void logout(String token) {
        String sessionId = JwtUtil.getSessionId(token);
        if (sessionId != null) {
            String sessionKey = CommonConstant.CACHE_SESSION + sessionId;
            redisTemplate.delete(sessionKey);
            log.info("[用户登出] Session已撤销，sessionId={}", sessionId);
        }
        // 将当前access token加入黑名单作为兜底
        long remainingTime = JwtUtil.getRemainingTime(token);
        if (remainingTime > 0) {
            String key = CommonConstant.CACHE_TOKEN_BLACKLIST + token;
            redisTemplate.opsForValue().set(key, "1", remainingTime, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 刷新访问令牌（滑动过期）
     *
     * <p>前端使用过期或即将过期的access token对应的refresh token换取新access token。
     * 成功后更新Redis Session的访问时间并重置TTL，实现滑动过期保活。</p>
     *
     * @param refreshToken 刷新令牌
     * @return 新的Token信息
     */
    @Override
    public TokenVO refresh(String refreshToken) {
        // 1. 校验refresh token有效性
        if (!JwtUtil.validateToken(refreshToken)) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }

        // 2. 校验token类型
        String tokenType = JwtUtil.getTokenType(refreshToken);
        if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new BizException(ResultCode.TOKEN_INVALID, "非法的刷新令牌");
        }

        // 3. 从Redis查询Session
        String sessionId = JwtUtil.getSessionId(refreshToken);
        if (sessionId == null) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        String sessionKey = CommonConstant.CACHE_SESSION + sessionId;
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        String userId = hashOps.get(sessionKey, "userId");
        if (userId == null) {
            throw new BizException(ResultCode.TOKEN_INVALID, "会话已失效，请重新登录");
        }

        // 4. 滑动过期：更新lastAccessTime，重置Session TTL
        hashOps.put(sessionKey, "lastAccessTime", String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(sessionKey, SESSION_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 5. 生成新的access token（保持同一sessionId）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", Long.valueOf(userId));
        claims.put("username", hashOps.get(sessionKey, "username"));
        claims.put("role", hashOps.get(sessionKey, "role"));
        String accessToken = JwtUtil.generateAccessToken(claims, sessionId);

        // 6. 返回新Token（refresh token不变，实现无感刷新）
        log.info("[Token刷新] Session保活成功，sessionId={}", sessionId);
        return new TokenVO(accessToken, refreshToken, ACCESS_TOKEN_EXPIRE_SECONDS);
    }

    /**
     * 将User实体转换为UserVO
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setRole(user.getRole());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
