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

    /**
     * @param req:
     *     private Long spaceId;
     *     private String title;
     *     private KnowledgeType type;   // DOC / MANUAL
     *     private String content;       // MANUAL: markdown；DOC: 解析纯文本(可后续补)
     *     private Long parentId;        // 可空：顶层
     *     private String ossKey;        // DOC 用（可空：先创建后上传也行）
     * @return Result<Knowledge>
     * @description 用户上传知识 手动+文档
     */
    @PostMapping
    public Result<Knowledge> createKnowledge(@RequestBody KnowledgeCreateRequestDto req) {
        return Result.success(knowledgeService.create(req));
    }

    /**
     * @param id: 要查看的知识id
     * @return Result<Knowledge>
     * @description 用户查看某知识
     */
    @GetMapping("/{id}")
    public Result<Knowledge> getKnowledge(@PathVariable Long id) {
        return Result.success(knowledgeService.get(id));
    }

    /**
     * @param id: 要修改的知识id
     * @param req:
     *     private String title;   // 知识名
     *     private String content; // 知识内容
     *     private Long parentId;  // 父知识id
     *     private String ossKey;  // osskey
     * @return Result<Knowledge>
     * @description 用户修改某条知识
     */
    @PutMapping("/{id}")
    public Result<Knowledge> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeUpdateRequestDto req) {
        return Result.success(knowledgeService.update(id, req));
    }

    /**
     * @param id:
     * @return Result<Void>
     * @description 用户删除某知识，会级联删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success(null,"Knowledge deleted");
    }

    /**
     * @param spaceId: space的id
     * @return Result<List<Knowledge>> 该space下的所有知识列表
     * @description 用户进入某 Space 时，获取该 Space 下的所有知识列表，包括子知识
     * 这里返回的List是排好序的：先父后子，同级按knowledgeId排序
     */
    @GetMapping("/space/{spaceId}")
    public Result<List<Knowledge>> listBySpace(@PathVariable Long spaceId) {
        return Result.success(knowledgeService.listBySpace(spaceId));
    }

    /**
     * @param spaceId: space的id
     * @param parentId: 可空，父节点id
     * @return Result<List<Knowledge>>
     * @description 用户在空间中点开某个目录/节点，
     * 加载它的直接子节点
     * 如果不传 parentId，加载顶层目录
     */
    @GetMapping("/space/{spaceId}/children")
    public Result<List<Knowledge>> listChildren(@PathVariable Long spaceId,
                                        @RequestParam(required = false) Long parentId) {
        return Result.success(knowledgeService.listChildren(spaceId, parentId));
    }

    /**
     * @param spaceId: space的id
     * @return Result<List<KnowledgeTreeNode>>
     * @description 用户在空间中查看知识树结构
     */
    @GetMapping("/space/{spaceId}/tree")
    public Result<List<KnowledgeTreeNode>> tree(@PathVariable Long spaceId) {
        return Result.success(knowledgeService.tree(spaceId));
    }
}
