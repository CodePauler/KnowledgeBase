package com.knowledgebase.backend.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDownloadDto {
    private InputStream stream; // 文件输入流
    private long contentLength; // 文件大小
    private String contentType; // 文件类型
    private String filename;    // 文件名
}
