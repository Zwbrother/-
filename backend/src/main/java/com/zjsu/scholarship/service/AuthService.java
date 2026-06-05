package com.zjsu.scholarship.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.common.BusinessException;
import com.zjsu.scholarship.entity.SchoolAuthMock;
import com.zjsu.scholarship.entity.User;
import com.zjsu.scholarship.mapper.SchoolAuthMockMapper;
import com.zjsu.scholarship.mapper.UserMapper;
import com.zjsu.scholarship.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final SchoolAuthMockMapper authMockMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, SchoolAuthMockMapper authMockMapper,
                       PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.authMockMapper = authMockMapper;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(String account, String password) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getAccount, account));
        if (user == null) throw new BusinessException("账号不存在");
        if (!"ACTIVE".equals(user.getStatus())) throw new BusinessException("账号已冻结，请联系管理员");

        boolean passed;
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            passed = encoder.matches(password, user.getPasswordHash());
        } else {
            SchoolAuthMock mock = authMockMapper.selectById(account);
            passed = mock != null && mock.getInitialPassword().equals(password);
        }
        if (!passed) throw new BusinessException("账号或密码错误");

        String token = jwtUtil.generate(user.getId(), user.getAccount(), user.getRole(), user.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("account", user.getAccount());
        result.put("name", user.getName());
        result.put("role", user.getRole());
        result.put("usingInitialPassword", user.getPasswordHash() == null || user.getPasswordHash().isEmpty());
        return result;
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度至少 6 位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        boolean passed;
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            passed = encoder.matches(oldPassword, user.getPasswordHash());
        } else {
            SchoolAuthMock mock = authMockMapper.selectById(user.getAccount());
            passed = mock != null && mock.getInitialPassword().equals(oldPassword);
        }
        if (!passed) throw new BusinessException("原密码错误");

        user.setPasswordHash(encoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
