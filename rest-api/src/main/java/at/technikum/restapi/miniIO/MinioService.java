package at.technikum.restapi.miniIO;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import at.technikum.restapi.service.exception.DocumentProcessingException;
import at.technikum.restapi.service.exception.DocumentUploadException;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${MINIO_BUCKET:paperless-files}")
    private String bucketName;

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created bucket '{}'", bucketName);
            }
        } catch (Exception e) {
            throw new DocumentUploadException("Failed to ensure bucket: " + bucketName, e);
        }
    }

    public String upload(MultipartFile file) {
        try {
            ensureBucket();
            String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();
            try (InputStream in = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(in, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }
            log.info("Uploaded file '{}' to bucket '{}'", objectKey, bucketName);
            return objectKey;
        } catch (Exception e) {
            throw new DocumentUploadException("Failed to upload file to MinIO", e);
        }
    }

    public void delete(String objectKey) {
        try {
            boolean exists = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()) != null;

            if (exists) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .build());
                log.info("Deleted object '{}' from bucket '{}'", objectKey, bucketName);
            } else {
                log.warn("Object '{}' not found in bucket '{}'", objectKey, bucketName);
            }
        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to delete object '" + objectKey + "' from MinIO", e);
        }
    }

    public String generatePresignedUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expiryMinutes * 60) // seconds
                            .build());
        } catch (Exception e) {
            throw new DocumentUploadException("Failed to generate presigned URL for " + objectKey, e);
        }
    }
}
