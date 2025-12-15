package at.technikum.Batch_Processor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogImportService {

    private final DocumentDailyAccessRepository repository;
    private final XmlMapper xmlMapper;

    @Setter
    @Value("${batch.input-dir}")
    private Path inputDir;

    @Setter
    @Value("${batch.archive-dir}")
    private Path archiveDir;

    @Value("classpath:xsd/access-log.xsd")
    private Resource xsdResource;

    @Scheduled(cron = "${batch.import-cron:0 0 1 * * *}")
    public void importAccessLogs() throws IOException {
        log.info("Starting access log import");

        Files.createDirectories(archiveDir);
        Files.createDirectories(inputDir);

        final Map<Key, AggregatedEntry> aggregatedCounts = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.xml")) {
            for (final Path file : stream) {
                log.debug("Processing file {}", file.getFileName());

                try {
                    validateXml(file);

                    final AccessLogXml logXml = xmlMapper.readValue(file.toFile(), AccessLogXml.class);
                    processFile(logXml, aggregatedCounts);

                    archive(file);
                    log.info("Archived file {}", file.getFileName());
                } catch (final Exception e) {
                    log.warn("File {} is invalid and will be moved to failed", file.getFileName(), e);

                    final Path failedDir = inputDir.resolveSibling("failed");
                    Files.createDirectories(failedDir);

                    Files.move(
                        file,
                        failedDir.resolve(file.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        persist(aggregatedCounts);
        log.info("Access log import finished");
    }

    private void processFile(AccessLogXml logXml, Map<Key, AggregatedEntry> aggregated) {
        logXml.entries().forEach(entry -> {
            Key key = new Key(entry.documentId(), logXml.date());
            aggregated.merge(
                key,
                new AggregatedEntry(entry.accessCount(), logXml.source()),
                (left, right) -> left.add(right));
        });
    }

    @Transactional
    public void persist(Map<Key, AggregatedEntry> aggregated) {
        aggregated.forEach((key, payload) -> {
            DocumentDailyAccess entity = repository
                .findByDocumentIdAndAccessDate(key.documentId(), key.date())
                .orElseGet(() -> DocumentDailyAccess.builder()
                    .documentId(key.documentId())
                    .accessDate(key.date())
                    .accessCount(0)
                    .createdAt(LocalDateTime.now())
                    .build());

            entity.setAccessCount(entity.getAccessCount() + payload.count());
            if (payload.source() != null) {
                entity.setSource(payload.source());
            }
            repository.save(entity);
        });
    }

    private record AggregatedEntry(int count, String source) {
        AggregatedEntry add(AggregatedEntry other) {
            int newCount = this.count + other.count;
            String newSource = other.source != null ? other.source : this.source;
            return new AggregatedEntry(newCount, newSource);
        }
    }

    private void archive(final Path file) throws IOException {
        Files.move(
            file,
            archiveDir.resolve(file.getFileName()),
            StandardCopyOption.REPLACE_EXISTING);
    }

    private void validateXml(final Path xmlFile) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        try (var xsdStream = xsdResource.getInputStream()) {
            Validator validator = factory.newSchema(new StreamSource(xsdStream)).newValidator();

            validator.validate(new StreamSource(xmlFile.toFile()));
        }
    }

    private record Key(UUID documentId, java.time.LocalDate date) {
    }
}
