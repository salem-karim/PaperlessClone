package at.technikum.restapi.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.persistence.repository.SearchDocumentRepository;
import at.technikum.restapi.service.mapper.DocumentMapper;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSearchServiceImpl implements DocumentSearchService {

    private final SearchDocumentRepository searchDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final DocumentMapper mapper;

    @Override
    public void indexDocumentMetadata(final Document document) {
        try {
            // Map to SearchDocument (ocrText and summaryText will be null initially)
            final SearchDocument searchDocument = mapper.toSearchDocument(document);

            searchDocumentRepository.save(searchDocument);
            log.info("Indexed document metadata {} in ElasticSearch (status: {})",
                    document.getId(), document.getProcessingStatus());
        } catch (final Exception e) {
            log.error("Failed to index document metadata {} in ElasticSearch: {}",
                    document.getId(), e.getMessage(), e);
            // Don't throw - indexing failure shouldn't block the main workflow
        }
    }

    @Override
    public void updateDocumentAfterOcr(final Document document) {
        try {
            final SearchDocument searchDocument = mapper.toSearchDocument(document);

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

    @Override
    public void updateDocumentAfterGenAI(final Document document) {
        try {
            final SearchDocument searchDocument = mapper.toSearchDocument(document);

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

    @Override
    public void updateDocumentStatus(final Document document) {
        try {
            final SearchDocument searchDocument = mapper.toSearchDocument(document);

            searchDocumentRepository.save(searchDocument);
            log.info("Updated document {} status in ElasticSearch: {}",
                    document.getId(), document.getProcessingStatus());
        } catch (final Exception e) {
            log.error("Failed to update document {} status in ElasticSearch: {}",
                    document.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void deleteFromIndex(final UUID documentId) {
        try {
            searchDocumentRepository.deleteById(documentId);
            log.info("Deleted document {} from ElasticSearch index", documentId);
        } catch (final Exception e) {
            log.warn("Failed to delete document {} from ElasticSearch index: {}",
                    documentId, e.getMessage(), e);
        }
    }

    @Override
    public List<SearchDocument> search(final String queryString) {
        if (queryString == null || queryString.isBlank()) {
            log.info("Empty search query provided, return empty results");
            return Collections.emptyList();
        }

        try {
            log.info("Searching ElasticSearch for query: {}", queryString);
            final Query searchQuery = NativeQuery.builder()
                    .withQuery(q -> q
                            .multiMatch(m -> m
                                    .query(queryString)
                                    .fields(
                                            "title^3", // Title is most important (3x boost)
                                            "originalFilename^2", // Filename is important (2x boost)
                                            "summaryText^1.5", // Summary is fairly important (1.5x boost)
                                            "ocrText" // OCR text has normal weight
                                    )
                                    .type(TextQueryType.BestFields)
                                    .fuzziness("AUTO") // Allow typos
                                    .prefixLength(2) // Require first 2 chars to match exactly
                                    .operator(Operator.Or)))
                    .build();

            final var searchHits = elasticsearchOperations.search(searchQuery, SearchDocument.class);
            final var results = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .toList();

            log.info("Found {} documents matching query '{}'", results.size(), queryString);

            return results;
        } catch (final Exception e) {
            log.error("Failed to search documents for query '{}': {}", queryString, e.getMessage(), e);
            throw new RuntimeException("Error searching documents", e);
        }
    }
}
