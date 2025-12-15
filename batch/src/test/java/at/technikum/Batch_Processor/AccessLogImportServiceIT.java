package at.technikum.Batch_Processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class AccessLogImportServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("paperless")
            .withUsername("paperless")
            .withPassword("secret");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.batch.jdbc.initialize-schema", () -> "never");
        registry.add("batch.import-cron", () -> "*/2 * * * * *");
    }

    @Autowired
    AccessLogImportService service;

    @Autowired
    DocumentDailyAccessRepository repository;

    @Autowired
    ScheduledAnnotationBeanPostProcessor scheduler;

    @TempDir
    Path tempDir;

    Path inputDir;
    Path archiveDir;

    @BeforeEach
    void setupDirs() throws Exception {
        inputDir = tempDir.resolve("in");
        archiveDir = tempDir.resolve("archive");
        Files.createDirectories(inputDir);
        Files.createDirectories(archiveDir);

        service.setInputDir(inputDir);
        service.setArchiveDir(archiveDir);
    }

    @Test
    void scheduledJobPersistsAggregatedAccessCounts() throws Exception {
        Path xml = inputDir.resolve("access.xml");
        Files.writeString(xml, """
                <access-log date="2024-09-10">
                  <entry>
                    <documentId>%s</documentId>
                    <accessCount>3</accessCount>
                  </entry>
                </access-log>
                """.formatted(UUID.randomUUID()));

        scheduler.postProcessAfterInitialization(service, "accessLogImportService");

        Awaitility.await().untilAsserted(() -> assertThat(repository.findAll())
                .hasSize(1)
                .first()
                .satisfies(entity -> {
                    assertThat(entity.getAccessDate()).isEqualTo(LocalDate.parse("2024-09-10"));
                    assertThat(entity.getAccessCount()).isEqualTo(3);
                }));
    }
}
