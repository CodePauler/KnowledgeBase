package com.knowledgebase.backend.controller;

import com.knowledgebase.backend.dto.KnowledgeCreateRequestDto;
import com.knowledgebase.backend.dto.KnowledgeTreeNode;
import com.knowledgebase.backend.dto.KnowledgeUpdateRequestDto;
import com.knowledgebase.backend.entity.Knowledge;
import com.knowledgebase.backend.entity.Result;
import com.knowledgebase.backend.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    // 创建知识
    @PostMapping
    public Result<Knowledge> createKnowledge(@RequestBody KnowledgeCreateRequestDto req) {
        return Result.success(knowledgeService.create(req));
    }

    // 查询单条
    @GetMapping("/{id}")
    public Result<Knowledge> getKnowledge(@PathVariable Long id) {
        return Result.success(knowledgeService.get(id));
    }

    // 更新
    @PutMapping("/{id}")
    public Result<Knowledge> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeUpdateRequestDto req) {
        return Result.success(knowledgeService.update(id, req));
    }

    // 删除（会级联删子知识，取决于你的外键设置）
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success(null,"Knowledge deleted");
    }

    // Space 下列表（平铺）
    @GetMapping("/space/{spaceId}")
    public Result<List<Knowledge>> listBySpace(@PathVariable Long spaceId) {
        return Result.success(knowledgeService.listBySpace(spaceId));
    }

    // Space 下 children（按 parentId 查）
    @GetMapping("/space/{spaceId}/children")
    public Result<List<Knowledge>> listChildren(@PathVariable Long spaceId,
                                        @RequestParam(required = false) Long parentId) {
        return Result.success(knowledgeService.listChildren(spaceId, parentId));
    }

    // Space 下目录树
    @GetMapping("/space/{spaceId}/tree")
    public Result<List<KnowledgeTreeNode>> tree(@PathVariable Long spaceId) {
        return Result.success(knowledgeService.tree(spaceId));
    }
}