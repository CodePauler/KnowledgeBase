package com.knowledgebase.backend.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 阿里云OSS客户端工具类
 */
@Component
@Slf4j
public class OssClientUtil {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg");

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.folder:knowledge}")
    private String folder;

    /**
     * Upload file to OSS and return object key.
     */
    public String upload(MultipartFile file, String objectPrefix) {
        validateFile(file);
        String extension = getExtension(file.getOriginalFilename());
        String objectKey = buildObjectKey(objectPrefix, extension);

        OSS ossClient = null;
        try (InputStream inputStream = file.getInputStream()) {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectKey, inputStream);
            ossClient.putObject(putObjectRequest);
            return objectKey;
        } catch (IOException e) {
            log.error("Failed to read upload file", e);
            throw new RuntimeException("Failed to read upload file", e);
        } catch (Exception e) {
            log.error("Upload to OSS failed", e);
            throw new RuntimeException("Upload to OSS failed", e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported file type: " + extension);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Missing file extension");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (ext.isBlank()) {
            throw new IllegalArgumentException("Missing file extension");
        }
        return ext;
    }

    private String buildObjectKey(String objectPrefix, String extension) {
        StringBuilder key = new StringBuilder();
        if (folder != null && !folder.isBlank()) {
            key.append(folder.trim()).append("/");
        }
        if (objectPrefix != null && !objectPrefix.isBlank()) {
            key.append(objectPrefix.trim());
            if (!objectPrefix.endsWith("/")) {
                key.append("/");
            }
        }
        key.append(System.currentTimeMillis()).append("-").append(UUID.randomUUID());
        key.append(".").append(extension);
        return key.toString();
    }
}
