package com.knowledgebase.backend.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
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
 * Azure Blob Storage 客户端封装
 * @description 支持文件上传到 Azure Blob Storage，返回文件 URL
 * 支持文件类型："pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"
 * 每个用户的文件分开存储
 * 形如 baseFolder/user-userId/category/timestamp-uuid.extension
 */
@Component
@Slf4j
public class AzureBlobClient {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg");

    @Value("${azure.blob.connection-string:}")
    private String connectionString;

    @Value("${azure.blob.container}")
    private String containerName;

    @Value("${azure.blob.base-folder:knowledge}")
    private String baseFolder;

    /**
     * @param file: 上传的文件
     * @param folder: 文件的分类文件夹，如common、avatar等
     * @param userId: userId 从JWT解析得到
     * @return String blob url
     * @description 上传文件到 Azure Blob Storage，返回文件 URL
     */
    public String upload(MultipartFile file, String folder, Long userId) {
        validateFile(file);
        String category = (folder == null || folder.isBlank()) ? "common" : folder.trim(); // 默认上传的是文档文件，存common
        String extension = getExtension(file.getOriginalFilename());    // 取后缀
        String blobName = buildBlobName(category, userId, extension);   // 构建文件名

        BlobServiceClient serviceClient = buildServiceClient();
        BlobContainerClient containerClient = getOrCreateContainer(serviceClient);
        BlobClient blobClient = new BlobClientBuilder()
                .endpoint(serviceClient.getAccountUrl())
                .containerName(containerName)
                .blobName(blobName)
                .connectionString(connectionString)
                .buildClient();

        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true);
            return blobClient.getBlobUrl();
        } catch (IOException e) {
            log.error("Failed to read upload file", e);
            throw new RuntimeException("Failed to read upload file", e);
        } catch (BlobStorageException e) {
            log.error("Azure Blob upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Upload to Azure Blob failed", e);
        }
    }

    /**
     * @return BlobServiceClient
     * @description 构建服务客户端
     */
    private BlobServiceClient buildServiceClient() {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException("Azure connection string is missing");
        }
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    /**
     * @param serviceClient: BlobServiceClient
     * @return BlobContainerClient 容器客户端
     * @description 获取客户端，不存在则创建
     */
    private BlobContainerClient getOrCreateContainer(BlobServiceClient serviceClient) {
        BlobContainerClient containerClient = new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient();
        if (!containerClient.exists()) {
            containerClient.create();
        }
        return containerClient;
    }

    /**
     * @param file: 上传的文件
     * @return void
     * @description 验证文件合法性
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported file type: " + extension);
        }
    }

    /**
     * @param filename: 文件名
     * @return String  文件后缀
     * @description 获取文件后缀
     */
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

    /**
     * @param category: 文件类型 common、avatar等
     * @param userId: userId JWT解析得到
     * @param extension: 后缀名
     * @return String   构建的唯一文件名 由baseFolder/user-userId/category/timestamp-uuid.extension组成
     * @description 根据用户ID和文件类型构建唯一文件名
     */
    private String buildBlobName(String category, Long userId, String extension) {
        StringBuilder name = new StringBuilder();
        if (baseFolder != null && !baseFolder.isBlank()) {
            name.append(baseFolder.trim()).append("/");
        }
        if (userId != null) {
            name.append("user-").append(userId).append("/");
        }
        name.append(category).append("/");
        name.append(System.currentTimeMillis())
                .append("-")
                .append(UUID.randomUUID())
                .append(".")
                .append(extension);
        return name.toString();
    }
}
