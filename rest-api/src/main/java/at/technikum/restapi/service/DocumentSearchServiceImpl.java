package at.technikum.restapi.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import co.elastic.clients.elasticsearch._types.FieldValue;
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
    public List<SearchDocument> search(final String queryString, final List<String> categoryNames) {
        final boolean hasQuery = queryString != null && !queryString.isBlank();
        final boolean hasCategories = categoryNames != null && !categoryNames.isEmpty();

        if (!hasQuery && !hasCategories) {
            log.info("Empty search query and no categories provided, return empty results");
            return Collections.emptyList();
        }

        try {
            final Query searchQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> {
                        if (hasQuery) {
                            b.must(m -> m.multiMatch(mm -> mm
                                    .query(queryString)
                                    .fields("title^3", "originalFilename^2", "summaryText^1.5", "ocrText")
                                    .type(TextQueryType.BestFields)
                                    .fuzziness("AUTO")
                                    .prefixLength(2)
                                    .operator(Operator.Or)));
                        }
                        if (hasCategories) {
                            b.filter(f -> f.terms(t -> t
                                    .field("categoryNames")
                                    .terms(v -> v.value(toFieldValues(categoryNames)))));
                        }
                        return b;
                    }))
                    .build();

            final var searchHits = elasticsearchOperations.search(searchQuery, SearchDocument.class);
            return searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .toList();
        } catch (final Exception e) {
            log.error("Failed to search documents for query '{}' and categories {}: {}",
                    queryString, categoryNames, e.getMessage(), e);
            throw new RuntimeException("Error searching documents", e);
        }
    }

    private List<FieldValue> toFieldValues(final List<String> values) {
        return values.stream()
                .map(FieldValue::of)
                .toList();
    }
}
