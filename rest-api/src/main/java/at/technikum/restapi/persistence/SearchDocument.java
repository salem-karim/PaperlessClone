package at.technikum.restapi.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import at.technikum.restapi.persistence.Document.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "documents") // Elasticsearch-Indexname
public class SearchDocument {

    @Id
    private UUID id;                    // gleiche ID wie bei JPA-Entity Document

    private String title;

    private String originalFilename;

    /**
     * Volltext-Inhalt (OCR-Text) auf dem später gesucht wird.
     */
    private String content;

    private Long fileSize;

    private String contentType;

    private ProcessingStatus processingStatus;

    private Instant createdAt;
}
