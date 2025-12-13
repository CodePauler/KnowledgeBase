package com.knowledgebase.backend.service;

import com.knowledgebase.backend.dao.SpaceMapper;
import com.knowledgebase.backend.entity.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description: 增删改查
 * TODO: 需增加space名唯一性的检查
 * @date: 2025/12/13 22:30
 */
@Service
@RequiredArgsConstructor
public class SpaceService {
    private final SpaceMapper spaceMapper;

    /**
     * @param userId: 用户id
     * @param name:   space名称
     * @param description: space描述
     * @return Space 返回创建的space对象
     * @date 2025/12/13 22:34
     */
    public Space createSpace(Long userId, String name, String description) {
        Space space = Space.builder()
                .userId(userId)
                .name(name)
                .description(description)
                .build();
        spaceMapper.insert(space);
        return space;
    }

    /**
     * @param id: space的id
     * @return Space 查找到的space对象
     * @date 2025/12/13 22:35
     */
    public Space getSpaceById(Long id) {
        return spaceMapper.selectById(id);
    }

    /**
     * @param userId: 用户ID
     * @return List<Space> 用户的所有space列表
     * @date 2025/12/13 22:36
     */
    public List<Space> listSpacesByUserId(Long userId) {
        return spaceMapper.selectByUserId(userId);
    }

    /**
     * @param id: space的id
     * @param name: space的名称
     * @param description: space的描述
     * @return void
     * @date 2025/12/13 22:38
     */
    public void updateSpace(Long id, String name, String description) {
        Space space = Space.builder()
                .id(id)
                .name(name)
                .description(description)
                .build();
        spaceMapper.updateById(space);
    }

    /**
     * @param id: space的id
     * @return void
     * @description 根据space的id删除space 这个应该可以硬删除
     * @date 2025/12/13 22:40
     */
    public void deleteSpace(Long id) {
        spaceMapper.deleteById(id);
    }
}
