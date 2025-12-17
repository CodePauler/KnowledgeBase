package com.knowledgebase.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageInterface {

    String upload(MultipartFile file, String category, Long userId);
}
