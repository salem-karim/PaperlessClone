package at.technikum.restapi.service;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    String uploadFile(MultipartFile file);

    InputStream downloadFile(String objectKey);

    void deleteFile(String objectKey);

    String uploadOcrText(String documentId, String text);

    String downloadOcrText(String objectKey);

    void deleteOcrText(String objectKey);

    String generatePresignedUrl(String objectKey, int expiryMinutes);
}
