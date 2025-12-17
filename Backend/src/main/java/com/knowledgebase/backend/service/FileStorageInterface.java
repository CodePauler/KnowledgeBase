package com.knowledgebase.backend.service;

import com.knowledgebase.backend.dto.FileUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageInterface {

    /**
     * @param file: 上传的文件
     * @param category: 文件类别
     * @param userId: 用户id
     * @return FileUploadResponseDto
     * @description 上传文件，返回响应DTO（blobname）
     */
    FileUploadResponseDto upload(MultipartFile file, String category, Long userId);

    /**
     * @param ossKey: 文件的blobname
     * @return FileDownloadDto
     * @description 根据blobname下载文件，返回下载DTO
     */
    FileDownloadDto download(String ossKey);
}
