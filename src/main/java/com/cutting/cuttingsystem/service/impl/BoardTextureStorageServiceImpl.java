package com.cutting.cuttingsystem.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.cutting.cuttingsystem.service.BoardTextureStorageService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BoardTextureStorageServiceImpl implements BoardTextureStorageService {
    private static final Logger log = LoggerFactory.getLogger(BoardTextureStorageServiceImpl.class);
    private static final long MAX_TEXTURE_BYTES = 5 * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final String endpoint;
    private final String bucket;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String objectPrefix;
    private final String publicBaseUrl;
    private final Object clientLock = new Object();
    private volatile OSS ossClient;

    public BoardTextureStorageServiceImpl(
            @Value("${app.oss.endpoint:}") String endpoint,
            @Value("${app.oss.bucket:}") String bucket,
            @Value("${app.oss.access-key-id:}") String accessKeyId,
            @Value("${app.oss.access-key-secret:}") String accessKeySecret,
            @Value("${app.oss.board-texture-prefix:board-textures/}") String objectPrefix,
            @Value("${app.oss.public-base-url:}") String publicBaseUrl
    ) {
        this.endpoint = normalizeEndpoint(endpoint);
        this.bucket = trimToEmpty(bucket);
        this.accessKeyId = trimToEmpty(accessKeyId);
        this.accessKeySecret = trimToEmpty(accessKeySecret);
        this.objectPrefix = normalizePrefix(objectPrefix);
        this.publicBaseUrl = trimTrailingSlash(trimToEmpty(publicBaseUrl));
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("texture file is required");
        }
        if (file.getSize() > MAX_TEXTURE_BYTES) {
            throw new IllegalArgumentException("texture file must be at most 5MB");
        }

        String extension = resolveExtension(file);
        ensureOssConfigured();

        String objectKey = objectPrefix + UUID.randomUUID() + extension;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        metadata.setCacheControl("public, max-age=31536000");

        long startedAt = System.nanoTime();
        try (InputStream inputStream = file.getInputStream()) {
            getOssClient().putObject(new PutObjectRequest(bucket, objectKey, inputStream, metadata));
        } catch (OSSException | ClientException e) {
            throw new IOException("upload texture to OSS failed", e);
        }
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("Uploaded board texture to OSS in {} ms, size={} bytes, key={}",
                elapsedMillis, file.getSize(), objectKey);
        return buildPublicUrl(objectKey);
    }

    @PreDestroy
    public void destroy() {
        OSS client = ossClient;
        if (client != null) {
            client.shutdown();
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            String extension = CONTENT_TYPE_EXTENSIONS.get(contentType.toLowerCase(Locale.ROOT));
            if (extension != null) return extension;
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("texture file must be jpg, png or webp");
        }
        String lowerName = filename.toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.stream()
                .filter(lowerName::endsWith)
                .findFirst()
                .map(extension -> ".jpeg".equals(extension) ? ".jpg" : extension)
                .orElseThrow(() -> new IllegalArgumentException("texture file must be jpg, png or webp"));
    }

    private void ensureOssConfigured() {
        if (endpoint.isBlank() || bucket.isBlank() || accessKeyId.isBlank() || accessKeySecret.isBlank()) {
            throw new IllegalArgumentException("OSS config is incomplete");
        }
    }

    private OSS getOssClient() {
        OSS client = ossClient;
        if (client == null) {
            synchronized (clientLock) {
                client = ossClient;
                if (client == null) {
                    client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                    ossClient = client;
                }
            }
        }
        return client;
    }

    private String buildPublicUrl(String objectKey) {
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl + "/" + objectKey;
        }
        String endpointWithoutScheme = endpoint
                .replaceFirst("^https?://", "")
                .replaceAll("/+$", "");
        String scheme = endpoint.startsWith("http://") ? "http://" : "https://";
        return scheme + bucket + "." + endpointWithoutScheme + "/" + objectKey;
    }

    private String normalizePrefix(String prefix) {
        String value = trimToEmpty(prefix).replace("\\", "/");
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value.endsWith("/") ? value : value + "/";
    }

    private String normalizeEndpoint(String value) {
        String endpointValue = trimToEmpty(value).replaceAll("/+$", "");
        if (endpointValue.isBlank() || endpointValue.startsWith("http://") || endpointValue.startsWith("https://")) {
            return endpointValue;
        }
        return "https://" + endpointValue;
    }

    private String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
