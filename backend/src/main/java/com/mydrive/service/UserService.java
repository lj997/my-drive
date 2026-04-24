package com.mydrive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydrive.dto.LoginDTO;
import com.mydrive.dto.RegisterDTO;
import com.mydrive.entity.User;
import com.mydrive.mapper.UserMapper;
import com.mydrive.util.JwtUtil;
import com.mydrive.util.Md5Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (!Md5Util.verify(loginDTO.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", buildUserInfo(user));
        return result;
    }

    public void register(RegisterDTO registerDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        User existUser = userMapper.selectOne(wrapper);

        if (existUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(Md5Util.encrypt(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setStorageLimit(10 * 1024 * 1024 * 1024L);
        user.setStorageUsed(0L);

        userMapper.insert(user);
    }

    public Map<String, Object> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return buildUserInfo(user);
    }

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("storageLimit", user.getStorageLimit());
        userInfo.put("storageUsed", user.getStorageUsed());
        return userInfo;
    }
}
