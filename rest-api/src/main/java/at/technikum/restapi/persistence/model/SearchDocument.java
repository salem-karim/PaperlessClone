package at.technikum.restapi.persistence.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import at.technikum.restapi.persistence.model.Document.ProcessingStatus;
import lombok.Builder;

@Builder
@Document(indexName = "documents") // Elasticsearch-Indexname
public record SearchDocument(
        @Id UUID id,
        // Searches using
        String title,
        String originalFilename,
        String ocrText,
        String summaryText,
        // Might filter by
        Long fileSize,
        String contentType,
        Instant createdAt,
        // Filter & easier mapping between SummaryDTO
        ProcessingStatus processingStatus) {
}
