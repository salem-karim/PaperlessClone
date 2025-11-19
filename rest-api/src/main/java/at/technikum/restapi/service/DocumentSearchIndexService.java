package at.technikum.restapi.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import at.technikum.restapi.miniIO.MinioService;
import at.technikum.restapi.persistence.Document;
import at.technikum.restapi.persistence.SearchDocument;
import at.technikum.restapi.persistence.SearchDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSearchIndexService {

    private final SearchDocumentRepository searchDocumentRepository;
    private final MinioService minioService;

    /**
     * Indexiert ein Dokument in Elasticsearch.
     * Holt sich den OCR-Text aus der Entity oder aus MinIO.
     */
    public void indexDocument(final Document document) {
        try {
            String content = document.getOcrText();

            if (content == null && document.getOcrTextObjectKey() != null) {
                // OCR-Text liegt in MinIO
                content = minioService.downloadOcrText(document.getOcrTextObjectKey());
            }

            if (content == null || content.isBlank()) {
                log.warn("Skipping indexing for document {} – OCR text is empty or null", document.getId());
                return;
            }

            final SearchDocument searchDocument = SearchDocument.builder()
                    .id(document.getId())
                    .title(document.getTitle())
                    .originalFilename(document.getOriginalFilename())
                    .content(content)
                    .fileSize(document.getFileSize())
                    .contentType(document.getContentType())
                    .processingStatus(document.getProcessingStatus())
                    .createdAt(document.getCreatedAt())
                    .build();

            searchDocumentRepository.save(searchDocument);
            log.info("Indexed document {} in Elasticsearch", document.getId());
        } catch (final Exception e) {
            log.error("Failed to index document {} in Elasticsearch: {}", document.getId(), e.getMessage(), e);
        }
    }

    /**
     * Entfernt ein Dokument aus dem Elasticsearch-Index.
     */
    public void deleteFromIndex(final UUID documentId) {
        try {
            searchDocumentRepository.deleteById(documentId);
            log.info("Deleted document {} from Elasticsearch index", documentId);
        } catch (final Exception e) {
            log.warn("Failed to delete document {} from Elasticsearch index: {}", documentId, e.getMessage(), e);
        }
    }
}
