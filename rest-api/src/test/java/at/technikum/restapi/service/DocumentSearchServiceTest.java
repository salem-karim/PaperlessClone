package at.technikum.restapi.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.persistence.repository.SearchDocumentRepository;
import at.technikum.restapi.service.mapper.DocumentMapper;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class DocumentSearchServiceTest {

    @Mock
    private SearchDocumentRepository searchDocumentRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private DocumentMapper mapper;

    @InjectMocks
    private DocumentSearchServiceImpl searchService;

    private Document testDocument;
    private SearchDocument testSearchDocument;

    @BeforeEach
    void setUp() {
        testDocument = Document.builder()
                .id(UUID.randomUUID())
                .title("Test Invoice")
                .originalFilename("invoice.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .fileBucket("test-bucket")
                .fileObjectKey("test-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.COMPLETED)
                .ocrText("This is OCR text content")
                .summaryText("This is a summary")
                .build();

        testSearchDocument = SearchDocument.builder()
                .id(testDocument.getId())
                .title(testDocument.getTitle())
                .originalFilename(testDocument.getOriginalFilename())
                .contentType(testDocument.getContentType())
                .processingStatus(testDocument.getProcessingStatus().name())
                .ocrText(testDocument.getOcrText())
                .summaryText(testDocument.getSummaryText())
                .createdAt(testDocument.getCreatedAt())
                .build();
    }

    @Test
    void testIndexDocumentMetadata_success() {
        // Given
        when(mapper.toSearchDocument(testDocument)).thenReturn(testSearchDocument);
        when(searchDocumentRepository.save(testSearchDocument)).thenReturn(testSearchDocument);

        // When
        searchService.indexDocumentMetadata(testDocument);

        // Then
        verify(mapper).toSearchDocument(testDocument);
        verify(searchDocumentRepository).save(testSearchDocument);
    }

    @Test
    void testIndexDocumentMetadata_handlesException() {
        // Given
        when(mapper.toSearchDocument(testDocument)).thenThrow(new RuntimeException("Mapping failed"));

        // When & Then - should not throw, just log error
        assertDoesNotThrow(() -> searchService.indexDocumentMetadata(testDocument));
        verify(searchDocumentRepository, never()).save(any());
    }

    @Test
    void testUpdateDocumentAfterOcr_success() {
        // Given
        when(mapper.toSearchDocument(testDocument)).thenReturn(testSearchDocument);
        when(searchDocumentRepository.save(testSearchDocument)).thenReturn(testSearchDocument);

        // When
        searchService.updateDocumentAfterOcr(testDocument);

        // Then
        verify(mapper).toSearchDocument(testDocument);
        verify(searchDocumentRepository).save(testSearchDocument);
    }

    @Test
    void testUpdateDocumentAfterOcr_handlesException() {
        // Given
        when(mapper.toSearchDocument(testDocument)).thenThrow(new RuntimeException("Update failed"));

        // When & Then - should not throw, just log error
        assertDoesNotThrow(() -> searchService.updateDocumentAfterOcr(testDocument));
        verify(searchDocumentRepository, never()).save(any());
    }

    @Test
    void testUpdateDocumentAfterGenAI_success() {
        // Given
        when(mapper.toSearchDocument(testDocument)).thenReturn(testSearchDocument);
        when(searchDocumentRepository.save(testSearchDocument)).thenReturn(testSearchDocument);

        // When
        searchService.updateDocumentAfterGenAI(testDocument);

        // Then
        verify(mapper).toSearchDocument(testDocument);
        verify(searchDocumentRepository).save(testSearchDocument);
    }

    @Test
    void testUpdateDocumentAfterGenAI_handlesException() {
        // Given
        when(mapper.toSearchDocument(testDocument)).thenThrow(new RuntimeException("Update failed"));

        // When & Then - should not throw, just log error
        assertDoesNotThrow(() -> searchService.updateDocumentAfterGenAI(testDocument));
        verify(searchDocumentRepository, never()).save(any());
    }

    @Test
    void testUpdateDocumentStatus_success() {
        // Given
        when(mapper.toSearchDocument(testDocument)).thenReturn(testSearchDocument);
        when(searchDocumentRepository.save(testSearchDocument)).thenReturn(testSearchDocument);

        // When
        searchService.updateDocumentStatus(testDocument);

        // Then
        verify(mapper).toSearchDocument(testDocument);
        verify(searchDocumentRepository).save(testSearchDocument);
    }

    @Test
    void testUpdateDocumentStatus_handlesException() {
        // Given
        when(mapper.toSearchDocument(testDocument)).thenThrow(new RuntimeException("Update failed"));

        // When & Then - should not throw, just log error
        assertDoesNotThrow(() -> searchService.updateDocumentStatus(testDocument));
        verify(searchDocumentRepository, never()).save(any());
    }

    @Test
    void testDeleteFromIndex_success() {
        // Given
        UUID documentId = UUID.randomUUID();

        // When
        searchService.deleteFromIndex(documentId);

        // Then
        verify(searchDocumentRepository).deleteById(documentId);
    }

    @Test
    void testDeleteFromIndex_handlesException() {
        // Given
        UUID documentId = UUID.randomUUID();
        doThrow(new RuntimeException("Delete failed")).when(searchDocumentRepository).deleteById(documentId);

        // When & Then - should not throw, just log warning
        assertDoesNotThrow(() -> searchService.deleteFromIndex(documentId));
    }

    @Test
    void testSearch_withEmptyQuery_returnsEmptyList() {
        // When
        List<SearchDocument> results = searchService.search("");

        // Then
        assertTrue(results.isEmpty());
        verify(elasticsearchOperations, never()).search(any(Query.class), eq(SearchDocument.class));
    }

    @Test
    void testSearch_withNullQuery_returnsEmptyList() {
        // When
        List<SearchDocument> results = searchService.search(null);

        // Then
        assertTrue(results.isEmpty());
        verify(elasticsearchOperations, never()).search(any(Query.class), eq(SearchDocument.class));
    }

    @Test
    void testSearch_withValidQuery_returnsResults() {
        // Given
        String query = "invoice";
        final SearchHit<SearchDocument> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(testSearchDocument);

        SearchHits<SearchDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));

        when(elasticsearchOperations.search(any(Query.class), eq(SearchDocument.class)))
                .thenReturn(searchHits);

        // When
        List<SearchDocument> results = searchService.search(query);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(testSearchDocument.id(), results.getFirst().id()); // ← Fixed: use record accessor
        verify(elasticsearchOperations).search(any(Query.class), eq(SearchDocument.class));
    }

    @Test
    void testSearch_withValidQuery_noResults() {
        // Given
        String query = "nonexistent";

        SearchHits<SearchDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(Collections.emptyList());

        when(elasticsearchOperations.search(any(Query.class), eq(SearchDocument.class)))
                .thenReturn(searchHits);

        // When
        List<SearchDocument> results = searchService.search(query);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(elasticsearchOperations).search(any(Query.class), eq(SearchDocument.class));
    }

    @Test
    void testSearch_handlesException() {
        // Given
        String query = "invoice";
        when(elasticsearchOperations.search(any(Query.class), eq(SearchDocument.class)))
                .thenThrow(new RuntimeException("Search failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> searchService.search(query));
    }

    @Test
    void testSearch_withWhitespaceQuery_returnsEmptyList() {
        // When
        List<SearchDocument> results = searchService.search("   ");

        // Then
        assertTrue(results.isEmpty());
        verify(elasticsearchOperations, never()).search(any(Query.class), eq(SearchDocument.class));
    }
}
