package at.technikum.restapi.service;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration",
    "spring.rabbitmq.listener.simple.auto-startup=false"
})
@Testcontainers
@ActiveProfiles("test")
class MinioServiceIntegrationTest {

    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("minioadmin")
            .withPassword("minioadmin");

    @TestConfiguration
    static class TestMinioConfiguration {
        @Bean
        @Primary
        public MinioClient minioClient() {
            return MinioClient.builder()
                    .endpoint(minioContainer.getS3URL())
                    .credentials(minioContainer.getUserName(), minioContainer.getPassword())
                    .build();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.url", minioContainer::getS3URL);
        registry.add("minio.access-key", minioContainer::getUserName);
        registry.add("minio.secret-key", minioContainer::getPassword);
        registry.add("minio.bucket-name", () -> "test-documents");
    }

    @Autowired
    private MinioServiceImpl minioService;

    @Autowired
    private MinioClient minioClient;

    @MockitoBean
    private DocumentSearchService documentSearchService;
    
    @BeforeEach
    void setup() throws Exception {
        // Ensure bucket exists
        String bucketName = "test-documents";
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    io.minio.MakeBucketArgs.builder().bucket(bucketName).build()
            );
        }
    }

    @Test
    void testUploadFile_success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Test PDF content for MinIO upload".getBytes()
        );

        // When
        String objectKey = minioService.uploadFile(file);

        // Then
        assertThat(objectKey).isNotNull();
        assertThat(objectKey).contains("test-document");
        assertThat(objectKey).endsWith(".pdf");

        // Verify file exists in MinIO
        InputStream downloadedStream = minioService.downloadFile(objectKey);
        assertThat(downloadedStream).isNotNull();
        String content = new String(downloadedStream.readAllBytes());
        assertThat(content).isEqualTo("Test PDF content for MinIO upload");
    }

    @Test
    void testUploadFile_differentFileTypes() throws Exception {
        // Test PNG
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", "image.png", "image/png", "PNG data".getBytes()
        );
        String pngKey = minioService.uploadFile(pngFile);
        assertThat(pngKey).endsWith(".png");

        // Test JPEG
        MockMultipartFile jpegFile = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "JPEG data".getBytes()
        );
        String jpegKey = minioService.uploadFile(jpegFile);
        assertThat(jpegKey).endsWith(".jpg");
    }

    @Test
    void testDownloadFile_success() throws Exception {
        // Given - upload a file first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "download-test.pdf",
                "application/pdf",
                "Content to download".getBytes()
        );
        String objectKey = minioService.uploadFile(file);

        // When
        InputStream downloadedStream = minioService.downloadFile(objectKey);

        // Then
        assertThat(downloadedStream).isNotNull();
        String content = new String(downloadedStream.readAllBytes());
        assertThat(content).isEqualTo("Content to download");
    }

    @Test
    void testDownloadFile_notFound() {
        // When/Then
        assertThatThrownBy(() -> minioService.downloadFile("non-existent-key.pdf"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testDeleteFile_success() throws Exception {
        // Given - upload a file first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to-delete.pdf",
                "application/pdf",
                "Will be deleted".getBytes()
        );
        String objectKey = minioService.uploadFile(file);

        // Verify it exists
        InputStream stream = minioService.downloadFile(objectKey);
        assertThat(stream).isNotNull();
        stream.close();

        // When
        minioService.deleteFile(objectKey);

        // Then - file should no longer exist
        assertThatThrownBy(() -> minioService.downloadFile(objectKey))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testGeneratePresignedUrl_success() throws Exception {
        // Given - upload a file first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "presigned-test.pdf",
                "application/pdf",
                "Presigned URL test content".getBytes()
        );
        String objectKey = minioService.uploadFile(file);

        // When
        String presignedUrl = minioService.generatePresignedUrl(objectKey, 15);

        // Then
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).startsWith("http");
        assertThat(presignedUrl).contains(objectKey);
        assertThat(presignedUrl).contains("X-Amz-Expires=900"); // 15 minutes = 900 seconds
    }

    @Test
    void testUploadLargeFile_success() throws Exception {
        // Given - create a 5MB file
        byte[] largeContent = new byte[5 * 1024 * 1024];
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-file.pdf",
                "application/pdf",
                largeContent
        );

        // When
        String objectKey = minioService.uploadFile(largeFile);

        // Then
        assertThat(objectKey).isNotNull();

        // Verify file was uploaded correctly
        InputStream downloadedStream = minioService.downloadFile(objectKey);
        byte[] downloadedContent = downloadedStream.readAllBytes();
        assertThat(downloadedContent).hasSize(5 * 1024 * 1024);
        assertThat(downloadedContent).isEqualTo(largeContent);
    }

    @Test
    void testUploadFile_withSpecialCharactersInFilename() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test file with spaces & special!chars.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        // When
        String objectKey = minioService.uploadFile(file);

        // Then
        assertThat(objectKey).isNotNull();
        
        // Verify can download
        InputStream stream = minioService.downloadFile(objectKey);
        assertThat(stream).isNotNull();
        String content = new String(stream.readAllBytes());
        assertThat(content).isEqualTo("Test content");
    }
}