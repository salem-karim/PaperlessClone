package at.technikum.restapi.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.persistence.repository.SearchDocumentRepository;
import at.technikum.restapi.service.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSearchService {

    private final SearchDocumentRepository searchDocumentRepository;
    private final DocumentMapper documentMapper;

    /**
     * Index document metadata immediately on upload (before OCR/GenAI processing).
     * This allows searching by title, filename, etc. even while processing.
     */
    public void indexDocumentMetadata(final Document document) {
        try {
            // Map to SearchDocument (ocrText and summaryText will be null initially)
            final SearchDocument searchDocument = documentMapper.toSearchDocument(document);

            searchDocumentRepository.save(searchDocument);
            log.info("Indexed document metadata {} in ElasticSearch (status: {})",
                    document.getId(), document.getProcessingStatus());
        } catch (final Exception e) {
            log.error("Failed to index document metadata {} in ElasticSearch: {}",
                    document.getId(), e.getMessage(), e);
            // Don't throw - indexing failure shouldn't block the main workflow
        }
    }

    /**
     * Update document in ElasticSearch after OCR completion.
     * Updates OCR text and processing status.
     */
    public void updateDocumentAfterOcr(final Document document) {
        try {
            final SearchDocument searchDocument = documentMapper.toSearchDocument(document);

            searchDocumentRepository.save(searchDocument);
            log.info("Updated document {} in ElasticSearch after OCR (status: {}, text: {} chars)",
                    document.getId(),
                    document.getProcessingStatus(),
                    document.getOcrText() != null ? document.getOcrText().length() : 0);
        } catch (final Exception e) {
            log.error("Failed to update document {} in ElasticSearch after OCR: {}",
                    document.getId(), e.getMessage(), e);
        }
    }

    /**
     * Update document in ElasticSearch after GenAI completion.
     * Updates summary text and processing status.
     */
    public void updateDocumentAfterGenAI(final Document document) {
        try {
            final SearchDocument searchDocument = documentMapper.toSearchDocument(document);

            searchDocumentRepository.save(searchDocument);
            log.info("Updated document {} in ElasticSearch after GenAI (status: {}, summary: {} chars)",
                    document.getId(),
                    document.getProcessingStatus(),
                    document.getSummaryText() != null ? document.getSummaryText().length() : 0);
        } catch (final Exception e) {
            log.error("Failed to update document {} in ElasticSearch after GenAI: {}",
                    document.getId(), e.getMessage(), e);
        }
    }

    /**
     * Update document status in ElasticSearch when processing fails.
     */
    public void updateDocumentStatus(final Document document) {
        try {
            final SearchDocument searchDocument = documentMapper.toSearchDocument(document);

            searchDocumentRepository.save(searchDocument);
            log.info("Updated document {} status in ElasticSearch: {}",
                    document.getId(), document.getProcessingStatus());
        } catch (final Exception e) {
            log.error("Failed to update document {} status in ElasticSearch: {}",
                    document.getId(), e.getMessage(), e);
        }
    }

    /**
     * Remove document from ElasticSearch index.
     */
    public void deleteFromIndex(final UUID documentId) {
        try {
            searchDocumentRepository.deleteById(documentId);
            log.info("Deleted document {} from ElasticSearch index", documentId);
        } catch (final Exception e) {
            log.warn("Failed to delete document {} from ElasticSearch index: {}",
                    documentId, e.getMessage(), e);
        }
    }

    /**
     * Search documents by query string.
     * Searches across title, filename, OCR text, and summary text.
     */
    public List<SearchDocument> search(final String query) {
        try {
            // TODO: Implement full-text search query
            // For now, return empty list - will implement in next step
            log.info("Searching ElasticSearch for query: {}", query);
            return List.of();
        } catch (final Exception e) {
            log.error("Failed to search documents for query '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Error searching documents", e);
        }
    }
}
