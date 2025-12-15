package at.technikum.Batch_Processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AccessLogImportServiceTest {

    @Autowired
    private AccessLogImportService service;

    @Autowired
    private DocumentDailyAccessRepository repository;

    @TempDir
    Path tempInputDir;

    @TempDir
    Path tempArchiveDir;

    @BeforeEach
    void setup() throws IOException {
        repository.deleteAll();
        service.setInputDir(tempInputDir);
        service.setArchiveDir(tempArchiveDir);
        copySampleXmlFiles();
    }

    private void copySampleXmlFiles() throws IOException {
        Path xmlResourceDir = Path.of("src/test/resources/xml");
        if (Files.exists(xmlResourceDir)) {
            try (var stream = Files.list(xmlResourceDir)) {
                stream.forEach(file -> {
                    try {
                        Files.copy(file, tempInputDir.resolve(file.getFileName()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private void clearDirectory(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            stream.forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    void aggregatesAccessCountsPerDocumentAndDate() throws Exception {
        service.importAccessLogs();
        assertTrue(Files.list(tempArchiveDir).count() > 0, "Files should be archived");

        UUID docId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDate date = LocalDate.parse("2025-12-13");

        DocumentDailyAccess entity = repository.findByDocumentIdAndAccessDate(docId1, date)
                .orElseThrow(() -> new AssertionError("Entity not found in DB"));

        assertEquals(8, entity.getAccessCount());
    }

    @Test
    void existingEntriesAreIncremented() throws Exception {
        UUID docId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDate date = LocalDate.parse("2025-12-13");

        repository.save(DocumentDailyAccess.builder()
                .documentId(docId1)
                .accessDate(date)
                .accessCount(2)
                .createdAt(LocalDateTime.now())
                .build());

        service.importAccessLogs();

        DocumentDailyAccess entity = repository.findByDocumentIdAndAccessDate(docId1, date)
                .orElseThrow(() -> new AssertionError("Entity not found in DB"));

        assertEquals(10, entity.getAccessCount());
    }

    @Test
    void createsSeparateEntriesPerDate() throws Exception {
        Path extraFile = tempInputDir.resolve("valid-access-log-extra.xml");
        Files.writeString(extraFile, """
                <access-log date="2025-12-14" source="external-system-C">
                  <entry>
                    <documentId>11111111-1111-1111-1111-111111111111</documentId>
                    <accessCount>2</accessCount>
                  </entry>
                </access-log>
                """);

        service.importAccessLogs();

        UUID docId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        DocumentDailyAccess day13 = repository.findByDocumentIdAndAccessDate(docId1, LocalDate.parse("2025-12-13"))
                .orElseThrow(() -> new AssertionError("Day 13 entity missing"));
        DocumentDailyAccess day14 = repository.findByDocumentIdAndAccessDate(docId1, LocalDate.parse("2025-12-14"))
                .orElseThrow(() -> new AssertionError("Day 14 entity missing"));

        assertEquals(8, day13.getAccessCount());
        assertEquals(2, day14.getAccessCount());
    }

    @Test
    void invalidXmlIsMovedAndNotPersisted() throws Exception {
        clearDirectory(tempInputDir);
        Path invalidFile = tempInputDir.resolve("invalid.xml");
        Files.writeString(invalidFile, "<access-log><entry><documentId>XXX</documentId></entry></access-log>");

        service.importAccessLogs();

        Path failedDir = tempInputDir.resolveSibling("failed");
        assertTrue(Files.exists(failedDir.resolve("invalid.xml")), "Invalid XML should be moved to failed");
        assertEquals(0, repository.count(), "No entries should be persisted");
    }
}
