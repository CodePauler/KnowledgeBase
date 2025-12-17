package com.knowledgebase.backend.service;

import com.knowledgebase.backend.dto.FileUploadResponseDto;
import com.knowledgebase.backend.service.FileDownloadDto;
import com.knowledgebase.backend.utils.AzureBlobClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AzureBlobStorageService implements FileStorageInterface {

    private final AzureBlobClient azureBlobClient;

    /**
     * 
     * @param file: 上传的文件
     * @param category: 分类文件类别
     * @param userId: 用户id
     * @return String 上传后文件的访问url
     */
    @Override
    public FileUploadResponseDto upload(MultipartFile file, String category, Long userId) {
        return azureBlobClient.upload(file, category, userId);
    }

    @Override
    public FileDownloadDto download(String ossKey) {
        return azureBlobClient.download(ossKey);
    }
}
