package com.knowledgebase.backend.service;

import com.knowledgebase.backend.dao.UserMapper;
import com.knowledgebase.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @description: TODO
 * @date: 2025/12/11 23:22
 * @version: 1.0
 */
@Service
@RequiredArgsConstructor // 为注入类自动生成构造函数，取代@Autowired
public class UserService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * @param username: 注册用户名
     * @param password: 注册密码-未加密
     * @param email:    注册邮箱
     * @return User
     * @TODO 登录
     * @HACK 注册未对密码加密
     * @date 2025/12/11 23:29
     */
    public User register(String username, String password, String email){
        // 检查是否已存在
        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .build();
        userMapper.insert(user);
        return user;
    }

    /**
     * @param username:
     * @param password: 原始密码
     * @return User
     * @description TODO
     * @date 2025/12/11 23:53
     */
    public User login(String username, String password){
        User user = userMapper.selectByUsername(username);
        if(user == null) {
            throw new RuntimeException("用户不存在");
        }
        if(!user.getPassword().equals(password)){
            throw new RuntimeException("密码错误");
        }
        return user;
    }
}

