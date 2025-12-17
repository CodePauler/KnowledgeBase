package com.knowledgebase.backend.controller;

import com.knowledgebase.backend.entity.Result;
import com.knowledgebase.backend.service.FileStorageInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器：所有上传统一走这里，便于拓展
 * @TODO: 后续可以增加文件删除接口以及鉴权
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileStorageInterface fileStorageService;

    /**
     * @param userId: 用户id（从JWT解析）
     * @param category: 分类文件夹，如 knowledge/avatar ，这里就传avatar
     * @param file: 上传的文件
     * @return Result<String> blob url
     * @description 支持 pdf/doc/docx/txt/png/jpg/jpeg 上传，返回 Azure Blob 的访问 URL
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestAttribute Long userId,
                                 @RequestParam(defaultValue = "common") String category,
                                 @RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.upload(file, category, userId);
            return Result.success(url, "File uploaded");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Upload file failed", e);
            return Result.error(e.getMessage());
        }
    }
}
