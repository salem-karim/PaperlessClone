package at.technikum.restapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.persistence.repository.DocumentRepository;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.exception.DocumentNotFoundException;
import at.technikum.restapi.service.mapper.DocumentMapper;
import at.technikum.restapi.service.messaging.publisher.DocumentPublisher;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;

    @Mock
    private DocumentMapper mapper;

    @Mock
    private DocumentPublisher publisher;

    @Mock
    private MinioService minioService;

    @Mock
    private DocumentSearchService documentSearchService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private Document testDocument;
    private DocumentSummaryDto testSummaryDto;
    private DocumentDetailDto testDetailDto;

    @BeforeEach
    void setUp() {
        testDocument = Document.builder()
                .id(UUID.randomUUID())
                .title("Test Document")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .fileBucket("test-bucket")
                .fileObjectKey("test-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build();

        testSummaryDto = DocumentSummaryDto.builder()
                .id(testDocument.getId())
                .title(testDocument.getTitle())
                .originalFilename(testDocument.getOriginalFilename())
                .contentType(testDocument.getContentType())
                .fileSize(testDocument.getFileSize())
                .processingStatus(testDocument.getProcessingStatus())
                .createdAt(testDocument.getCreatedAt())
                .build();

        testDetailDto = DocumentDetailDto.builder()
                .id(testDocument.getId())
                .title(testDocument.getTitle())
                .originalFilename(testDocument.getOriginalFilename())
                .contentType(testDocument.getContentType())
                .fileSize(testDocument.getFileSize())
                .fileObjectKey(testDocument.getFileObjectKey())
                .processingStatus(testDocument.getProcessingStatus())
                .createdAt(testDocument.getCreatedAt())
                .build();
    }

    @Test
    void testGetAll_returnsAllDocuments() {
        // Given
        when(repository.findAll()).thenReturn(List.of(testDocument));
        when(mapper.toSummaryDto(testDocument)).thenReturn(testSummaryDto);

        // When
        final List<DocumentSummaryDto> results = documentService.getAll();

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(testSummaryDto.id(), results.getFirst().id());
        verify(repository).findAll();
    }

    @Test
    void testGetById_found() {
        // Given
        when(repository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));
        when(mapper.toDetailDto(testDocument)).thenReturn(testDetailDto);

        // When
        final DocumentDetailDto result = documentService.getById(testDocument.getId());

        // Then
        assertNotNull(result);
        assertEquals(testDetailDto.id(), result.id());
        verify(repository).findById(testDocument.getId());
    }

    @Test
    void testGetById_notFound() {
        // Given
        final UUID randomId = UUID.randomUUID();
        when(repository.findById(randomId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DocumentNotFoundException.class,
                () -> documentService.getById(randomId));
    }

    @Test
    void testUpdate_success() {
        // Given
        final DocumentSummaryDto updateDto = DocumentSummaryDto.builder()
                .id(testDocument.getId())
                .title("Updated Title")
                .originalFilename(testDocument.getOriginalFilename())
                .contentType(testDocument.getContentType())
                .fileSize(testDocument.getFileSize())
                .processingStatus(testDocument.getProcessingStatus())
                .createdAt(testDocument.getCreatedAt())
                .build();

        when(repository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));
        when(repository.save(any(Document.class))).thenReturn(testDocument);
        when(mapper.toSummaryDto(any(Document.class))).thenReturn(updateDto);

        // When
        final DocumentSummaryDto result = documentService.update(testDocument.getId(), updateDto);

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", result.title());
        verify(repository).save(any(Document.class));
        verify(documentSearchService).updateDocumentStatus(any(Document.class));
    }

    @Test
    void testDelete_success() {
        // Given
        when(repository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When
        documentService.delete(testDocument.getId());

        // Then
        verify(repository).deleteById(testDocument.getId());
        verify(minioService).deleteFile(testDocument.getFileObjectKey());
        verify(documentSearchService).deleteFromIndex(testDocument.getId());
    }

    @Test
    void testUpdateOcrResult_success() {
        // Given
        final UUID documentId = testDocument.getId();
        final String ocrText = "OCR text content";
        final String ocrTextKey = "ocr-text-key";

        when(repository.findById(documentId)).thenReturn(Optional.of(testDocument));
        when(repository.save(any(Document.class))).thenReturn(testDocument);

        // When
        documentService.updateOcrResult(documentId, ocrText, ocrTextKey);

        // Then
        verify(repository).save(argThat(doc -> doc.getProcessingStatus() == Document.ProcessingStatus.OCR_COMPLETED &&
                doc.getOcrText().equals(ocrText)));
        verify(documentSearchService).updateDocumentAfterOcr(any(Document.class));
        verify(publisher).publishDocumentForGenAI(any(Document.class));
    }

    @Test
    void testMarkOcrAsFailed_success() {
        // Given
        final UUID documentId = testDocument.getId();
        final String error = "OCR processing failed";

        when(repository.findById(documentId)).thenReturn(Optional.of(testDocument));
        when(repository.save(any(Document.class))).thenReturn(testDocument);

        // When
        documentService.markOcrAsFailed(documentId, error);

        // Then
        verify(repository).save(argThat(doc -> doc.getProcessingStatus() == Document.ProcessingStatus.OCR_FAILED &&
                doc.getProcessingError().equals(error)));
        verify(documentSearchService).updateDocumentStatus(any(Document.class));
    }

    @Test
    void testUpdateGenAIResult_success() {
        // Given
        final UUID documentId = testDocument.getId();
        final String summaryText = "This is a summary";

        testDocument.setProcessingStatus(Document.ProcessingStatus.GENAI_PROCESSING);
        when(repository.findById(documentId)).thenReturn(Optional.of(testDocument));
        when(repository.save(any(Document.class))).thenReturn(testDocument);

        // When
        documentService.updateGenAIResult(documentId, summaryText);

        // Then
        verify(repository).save(argThat(doc -> doc.getProcessingStatus() == Document.ProcessingStatus.COMPLETED &&
                doc.getSummaryText().equals(summaryText)));
        verify(documentSearchService).updateDocumentAfterGenAI(any(Document.class));
    }

    @Test
    void testSearch_delegatesToSearchService() {
        // Given
        final String query = "test";
        final SearchDocument searchDoc = SearchDocument.builder()
                .id(testDocument.getId())
                .title(testDocument.getTitle())
                .originalFilename(testDocument.getOriginalFilename())
                .contentType(testDocument.getContentType())
                .processingStatus(testDocument.getProcessingStatus().name())
                .createdAt(testDocument.getCreatedAt())
                .build();

        when(documentSearchService.search(query, new ArrayList<>())).thenReturn(List.of(searchDoc));
        when(mapper.toSummaryDto(searchDoc)).thenReturn(testSummaryDto);

        // When
        final List<DocumentSummaryDto> results = documentService.search(query, new ArrayList<>());

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(documentSearchService).search(query, new ArrayList<>());
    }
}
