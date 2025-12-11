package com.knowledgebase.backend.dao;

import com.knowledgebase.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
    user表的Mapper接口
 */
@Mapper
public interface UserMapper {
    // 添加用户 失败返回0
    int insert (User user);

    // 根据用户ID找用户 返回User
    User selectById(Long id);

    // 根据用户名找用户 返回User
    User selectByUsername(@Param("username") String username);

    // 根据用户ID更新用户信息 成功返回1
    int updateById(User user);

    // 根据用户ID删除用户信息 未来可能考虑改成伪删除
    int deleteById(Long id);
}
