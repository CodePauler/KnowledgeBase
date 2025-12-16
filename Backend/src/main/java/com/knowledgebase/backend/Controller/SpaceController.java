package com.knowledgebase.backend.controller;

import com.knowledgebase.backend.dto.CreateSpaceResponseDto;
import com.knowledgebase.backend.entity.Result;
import com.knowledgebase.backend.entity.Space;
import com.knowledgebase.backend.service.SpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: Space的增删改查，与AuthController几乎一致
 * @date: 2025/12/13 22:40
 */
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
@Slf4j
public class SpaceController {

    private final SpaceService spaceService;

    /**
     * @param userId: 用户id
     * @param name: space名称
     * @param description: space描述
     * @description 用户创建space
     * 这里采用认证信息与业务数据分离的设计，name和description通过请求参数传递，
     * 而userId通过@RequestAttribute从认证信息中获取，确保数据的完整性和安全性。
     * 这里@RequestAttribute注解的来源是HttpServletRequest.setAttribute()，
     * 是服务端在解析token后从token中提取出来的。
     * @date 2025/12/13 23:44
     */
    @PostMapping
    public Result<CreateSpaceResponseDto> createSpace(@RequestAttribute Long userId,
                                     @RequestParam String name,
                                     @RequestParam(required = false) String description) {
        try {
            Space space = spaceService.createSpace(userId, name, description);
            CreateSpaceResponseDto dto = CreateSpaceResponseDto.builder()
                    .name(space.getName())
                    .description(space.getDescription())
                    .build();
            return Result.success(dto, "Space created");
        } catch (Exception e) {
            log.error("Create space failed", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * @param id: space的id
     * @return Result<Space>
     * @TODO: 未来应该添加权限检查，确保用户只能访问自己的space
     * @description 用户点击一个space时将space id传入，获取space信息
     */
    @GetMapping("/{id}")
    public Result<Space> getSpace(@PathVariable Long id) {
        try {
            Space space = spaceService.getSpaceById(id);
            if (space == null) {
                return Result.notFound();
            }
            return Result.success(space);
        } catch (Exception e) {
            log.error("Get space failed", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * @param userId: 用户id
     * @return Result<List<Space>>
     * @description 用户进入网页时，获取该用户的所有space列表
     */
    @GetMapping
    public Result<List<Space>> listSpaces(@RequestAttribute Long userId) {
        try {
            List<Space> spaces = spaceService.listSpacesByUserId(userId);
            return Result.success(spaces);
        } catch (Exception e) {
            log.error("List spaces failed", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * @param id: Spaceid
     * @param name: new Space name
     * @param description: new Space description
     * @return Result<Void>
     * @description 更新Space信息（更新Space名称、描述）
     */
    @PutMapping("/{id}")
    public Result<Void> updateSpace(@PathVariable Long id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) String description) {
        try {
            spaceService.updateSpace(id, name, description);
            return Result.success(null, "Space updated");
        } catch (Exception e) {
            log.error("Update space failed", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * @param id: Space id
     * @return Result<Void>
     * @description 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSpace(@PathVariable Long id) {
        try {
            spaceService.deleteSpace(id);
            return Result.success(null, "Space deleted");
        } catch (Exception e) {
            log.error("Delete space failed", e);
            return Result.error(e.getMessage());
        }
    }
}
