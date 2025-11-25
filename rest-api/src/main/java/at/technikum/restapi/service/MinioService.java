package at.technikum.restapi.service;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    String uploadFile(final MultipartFile file);

    InputStream downloadFile(final String objectKey);

    void deleteFile(final String objectKey);

    String downloadOcrText(final String objectKey);

    void deleteOcrText(final String objectKey);

    String generatePresignedUrl(final String objectKey, final int expiryMinutes);
}
