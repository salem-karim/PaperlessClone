package at.technikum.restapi.miniIO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import at.technikum.restapi.service.exception.DocumentProcessingException;
import at.technikum.restapi.service.exception.DocumentUploadException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.documents-bucket:paperless-documents}")
    private String bucketName;

    @Value("${minio.ocr-text-bucket:paperless-ocr-text}")
    private String ocrTextBucketName;

    private void ensureBucket(final String bucket) {
        try {
            final boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created bucket '{}'", bucket);
            }
        } catch (final Exception e) {
            throw new DocumentUploadException("Failed to ensure bucket: " + bucket, e);
        }
    }

    public String upload(final MultipartFile file) {
        try {
            ensureBucket(bucketName);
            final String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();

            try (InputStream in = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(in, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }

            log.info("Uploaded document '{}' to bucket '{}'", objectKey, bucketName);
            return objectKey;
        } catch (final Exception e) {
            throw new DocumentUploadException("Failed to upload document to MinIO", e);
        }
    }

    public InputStream downloadFile(final String objectKey) {
        try {
            log.info("Downloading file: {}/{}", bucketName, objectKey);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build());
        } catch (final Exception e) {
            throw new DocumentProcessingException(
                    "Failed to download file from MinIO: " + objectKey, e);
        }
    }

    public void delete(final String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build());
            log.info("Deleted object '{}' from bucket '{}'", objectKey, bucketName);
        } catch (final Exception e) {
            log.warn("Failed to delete object '{}' from bucket '{}': {}",
                    objectKey, bucketName, e.getMessage());
        }
    }

    public void deleteOcrText(final String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(ocrTextBucketName)
                            .object(objectKey)
                            .build());
            log.info("Deleted OCR text '{}' from bucket '{}'", objectKey, ocrTextBucketName);
        } catch (final Exception e) {
            log.warn("Failed to delete OCR text '{}' from bucket '{}': {}",
                    objectKey, ocrTextBucketName, e.getMessage());
        }
    }

    public String downloadOcrText(final String objectKey) {
        try {
            ensureBucket(ocrTextBucketName);
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(ocrTextBucketName)
                            .object(objectKey)
                            .build())) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (final Exception e) {
            throw new DocumentProcessingException(
                    "Failed to download OCR text from MinIO: " + objectKey, e);
        }
    }

    public String uploadOcrText(final String documentId, final String ocrText) {
        try {
            ensureBucket(ocrTextBucketName);
            final String objectKey = "ocr/" + documentId + ".txt";

            final byte[] textBytes = ocrText.getBytes(StandardCharsets.UTF_8);
            try (InputStream in = new ByteArrayInputStream(textBytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(ocrTextBucketName)
                                .object(objectKey)
                                .stream(in, textBytes.length, -1)
                                .contentType("text/plain; charset=utf-8")
                                .build());
            }

            log.info("Uploaded OCR text '{}' to bucket '{}'", objectKey, ocrTextBucketName);
            return objectKey;
        } catch (final Exception e) {
            throw new DocumentUploadException("Failed to upload OCR text to MinIO", e);
        }
    }

    /**
     * Generates a presigned URL using the external client
     * This ensures the signature is calculated with the correct endpoint
     */
    public String generatePresignedUrl(final String objectKey, final int expiryMinutes) {
        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expiryMinutes * 60)
                            .build());

            if (!presignedUrl.contains("localhost:9000")) {
                presignedUrl = presignedUrl.replace("http://minio:9000/", "http://localhost:8000/minio/");
            }

            log.debug("Generated presigned URL for {}: {}", objectKey, presignedUrl);
            return presignedUrl;
        } catch (final Exception e) {
            log.error("Failed to generate presigned URL for {}: {}", objectKey, e.getMessage());
            throw new DocumentUploadException("Failed to generate presigned URL for " + objectKey, e);
        }
    }

    public String generateOcrTextPresignedUrl(final String objectKey, final int expiryMinutes) {
        try {
            final String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(ocrTextBucketName)
                            .object(objectKey)
                            .expiry(expiryMinutes * 60)
                            .build());

            log.debug("Generated presigned OCR text URL for {}: {}", objectKey, presignedUrl);
            return presignedUrl;
        } catch (final Exception e) {
            throw new DocumentUploadException("Failed to generate presigned URL for OCR text: " + objectKey, e);
        }
    }
}
