package com.knowledgebase.backend.controller;

import com.knowledgebase.backend.dto.FileUploadResponseDto;
import com.knowledgebase.backend.dto.KnowledgeCreateRequestDto;
import com.knowledgebase.backend.dto.KnowledgeTreeNode;
import com.knowledgebase.backend.dto.KnowledgeUpdateRequestDto;
import com.knowledgebase.backend.entity.Knowledge;
import com.knowledgebase.backend.entity.Result;
import com.knowledgebase.backend.service.FileStorageInterface;
import com.knowledgebase.backend.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final FileStorageInterface fileStorageService;

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

    /**
     * @param userId: 用户id
     * @param id: 知识id
     * @param category: 分类文件类别
     * @param file: 要上传的文档文件
     * @return Result<Knowledge>
     * @description 上传文档文件到azure blob 返回url
     */
    @PostMapping("/{id}/upload")
    public Result<FileUploadResponseDto> uploadKnowledgeFile(@RequestAttribute Long userId,
                                                 @PathVariable Long id,
                                                 @RequestParam(defaultValue = "knowledge") String category,
                                                 @RequestParam("file") MultipartFile file) {
        try {
            FileUploadResponseDto res = knowledgeService.uploadOssFile(id, file, category, userId);
            return Result.success(res, "Knowledge file uploaded");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * @param id 知识id
     * @return ResponseEntity<InputStreamResource>
     * @description 下载知识文件
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<InputStreamResource> downloadKnowledgeFile(@PathVariable Long id) {
        try {
            var fileDto = knowledgeService.getKnowledgeFile(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(fileDto.getContentType()));
            headers.setContentLength(fileDto.getContentLength());
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileDto.getFilename() + "\"");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(fileDto.getStream()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
