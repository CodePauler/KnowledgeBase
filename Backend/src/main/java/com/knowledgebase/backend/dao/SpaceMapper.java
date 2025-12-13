package com.knowledgebase.backend.dao;

import com.knowledgebase.backend.entity.Space;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SpaceMapper {
    /**添加Space*/
    int insert(Space space);

    /**根据space的id查找space*/
    Space selectById(Long id);

    /**根据用户id查找拥有的space*/
    List<Space> selectByUserId(@Param("userId") Long userId);

    /**更新space？*/
    int updateById(Space space);

    /**根据space的id删除space*/
    int deleteById(Long id);
}
