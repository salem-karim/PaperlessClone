package at.technikum.restapi.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfig {

    @Value("${MINIO_ENDPOINT:http://localhost:9000}")
    private String minioUrl;

    @Value("${MINIO_ACCESS_KEY:paperless}")
    private String accessKey;

    @Value("${MINIO_SECRET_KEY:paperless123}")
    private String secretKey;

    @Bean
    MinioClient internalMinioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}
