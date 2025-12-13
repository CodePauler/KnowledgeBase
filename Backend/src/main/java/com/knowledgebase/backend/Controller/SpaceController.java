package com.knowledgebase.backend.controller;

import com.knowledgebase.backend.dto.CreateSpaceResponseDto;
import com.knowledgebase.backend.entity.Result;
import com.knowledgebase.backend.entity.Space;
import com.knowledgebase.backend.service.SpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}
