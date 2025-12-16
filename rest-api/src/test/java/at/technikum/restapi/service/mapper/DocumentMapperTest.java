package at.technikum.restapi.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.messaging.dto.GenAIRequestDto;
import at.technikum.restapi.service.messaging.dto.OcrRequestDto;

class DocumentMapperTest {

    private final DocumentMapper mapper = Mappers.getMapper(DocumentMapper.class);

    // ========== toSummaryDto(Document) Tests ==========

    @Test
    void testToSummaryDto_allFields() {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Document document = Document.builder()
                .id(id)
                .title("Test Document")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .fileBucket("test-bucket")
                .fileObjectKey("test-key")
                .createdAt(now)
                .processingStatus(Document.ProcessingStatus.COMPLETED)
                .build();

        // When
        DocumentSummaryDto dto = mapper.toSummaryDto(document);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.title()).isEqualTo("Test Document");
        assertThat(dto.originalFilename()).isEqualTo("test.pdf");
        assertThat(dto.contentType()).isEqualTo("application/pdf");
        assertThat(dto.fileSize()).isEqualTo(12345L);
        assertThat(dto.createdAt()).isEqualTo(now);
        assertThat(dto.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
    }

    @Test
    void testToSummaryDto_minimalFields() {
        // Given
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .title("Minimal Doc")
                .originalFilename("minimal.pdf")
                .contentType("application/pdf")
                .fileSize(1000L)
                .fileBucket("bucket")
                .fileObjectKey("key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build();

        // When
        DocumentSummaryDto dto = mapper.toSummaryDto(document);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(document.getId());
        assertThat(dto.title()).isEqualTo("Minimal Doc");
        assertThat(dto.processingStatus()).isEqualTo(Document.ProcessingStatus.PENDING);
    }

    @Test
    void testToSummaryDto_nullDocument() {
        // When
        DocumentSummaryDto dto = mapper.toSummaryDto((Document) null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    void testToSummaryDto_allProcessingStatuses() {
        // Test all processing statuses
        for (Document.ProcessingStatus status : Document.ProcessingStatus.values()) {
            // Given
            Document document = Document.builder()
                    .id(UUID.randomUUID())
                    .title("Test")
                    .originalFilename("test.pdf")
                    .contentType("application/pdf")
                    .fileSize(1000L)
                    .fileBucket("bucket")
                    .fileObjectKey("key")
                    .createdAt(Instant.now())
                    .processingStatus(status)
                    .build();

            // When
            DocumentSummaryDto dto = mapper.toSummaryDto(document);

            // Then
            assertThat(dto.processingStatus()).isEqualTo(status);
        }
    }

    @Test
    void testToSummaryDto_largeFileSize() {
        // Given
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .title("Large File")
                .originalFilename("large.pdf")
                .contentType("application/pdf")
                .fileSize(1024L * 1024L * 1024L * 5L) // 5GB
                .fileBucket("bucket")
                .fileObjectKey("key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build();

        // When
        DocumentSummaryDto dto = mapper.toSummaryDto(document);

        // Then
        assertThat(dto.fileSize()).isEqualTo(1024L * 1024L * 1024L * 5L);
    }

    @Test
    void testToSummaryDto_specialCharacters() {
        // Given
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .title("Test: Document & File \"Name\"")
                .originalFilename("file with spaces & special.pdf")
                .contentType("application/pdf")
                .fileSize(1000L)
                .fileBucket("bucket")
                .fileObjectKey("key/with/slashes")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.COMPLETED)
                .build();

        // When
        DocumentSummaryDto dto = mapper.toSummaryDto(document);

        // Then
        assertThat(dto.title()).isEqualTo("Test: Document & File \"Name\"");
        assertThat(dto.originalFilename()).isEqualTo("file with spaces & special.pdf");
    }

    // ========== toSummaryDto(SearchDocument) Tests ==========

    @Test
    void testToSummaryDto_fromSearchDocument() {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        SearchDocument searchDoc = SearchDocument.builder()
                .id(id)
                .title("Search Result")
                .originalFilename("result.pdf")
                .contentType("application/pdf")
                .fileSize(5000L)
                .createdAt(now)
                .processingStatus("COMPLETED")
                .build();

        // When
        DocumentSummaryDto dto = mapper.toSummaryDto(searchDoc);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.title()).isEqualTo("Search Result");
        assertThat(dto.originalFilename()).isEqualTo("result.pdf");
        assertThat(dto.contentType()).isEqualTo("application/pdf");
        assertThat(dto.fileSize()).isEqualTo(5000L);
        assertThat(dto.createdAt()).isEqualTo(now);
    }

    @Test
    void testToSummaryDto_nullSearchDocument() {
        // When
        DocumentSummaryDto dto = mapper.toSummaryDto((SearchDocument) null);

        // Then
        assertThat(dto).isNull();
    }

    // ========== toDetailDto Tests ==========

    @Test
    void testToDetailDto_allFields() {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant ocrProcessed = now.plusSeconds(100);
        Instant genaiProcessed = now.plusSeconds(200);

        Document document = Document.builder()
                .id(id)
                .title("Detail Document")
                .originalFilename("detail.pdf")
                .contentType("application/pdf")
                .fileSize(8000L)
                .fileBucket("detail-bucket")
                .fileObjectKey("detail-key")
                .summaryText("Detail summary")
                .createdAt(now)
                .ocrProcessedAt(ocrProcessed)
                .genaiProcessedAt(genaiProcessed)
                .processingStatus(Document.ProcessingStatus.COMPLETED)
                .processingError(null)
                .build();

        // When
        DocumentDetailDto dto = mapper.toDetailDto(document);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.title()).isEqualTo("Detail Document");
        assertThat(dto.originalFilename()).isEqualTo("detail.pdf");
        assertThat(dto.contentType()).isEqualTo("application/pdf");
        assertThat(dto.fileSize()).isEqualTo(8000L);
        assertThat(dto.fileBucket()).isEqualTo("detail-bucket");
        assertThat(dto.fileObjectKey()).isEqualTo("detail-key");
        assertThat(dto.summaryText()).isEqualTo("Detail summary");
        assertThat(dto.createdAt()).isEqualTo(now);
        assertThat(dto.ocrProcessedAt()).isEqualTo(ocrProcessed);
        assertThat(dto.genaiProcessedAt()).isEqualTo(genaiProcessed);
        assertThat(dto.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(dto.processingError()).isNull();
    }

    @Test
    void testToDetailDto_withError() {
        // Given
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .title("Failed Doc")
                .originalFilename("failed.pdf")
                .contentType("application/pdf")
                .fileSize(5000L)
                .fileBucket("bucket")
                .fileObjectKey("key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.OCR_FAILED)
                .processingError("OCR processing failed")
                .build();

        // When
        DocumentDetailDto dto = mapper.toDetailDto(document);

        // Then
        assertThat(dto.processingStatus()).isEqualTo(Document.ProcessingStatus.OCR_FAILED);
        assertThat(dto.processingError()).isEqualTo("OCR processing failed");
    }

    @Test
    void testToDetailDto_nullDocument() {
        // When
        DocumentDetailDto dto = mapper.toDetailDto(null);

        // Then
        assertThat(dto).isNull();
    }

    // ========== toOcrRequestDto Tests ==========

    @Test
    void testToOcrRequestDto_mapsDocumentIdCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .id(id)
                .title("OCR Request")
                .originalFilename("request.pdf")
                .contentType("application/pdf")
                .fileSize(3000L)
                .fileBucket("ocr-bucket")
                .fileObjectKey("ocr-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build();

        // When
        OcrRequestDto dto = mapper.toOcrRequestDto(document);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.documentId()).isEqualTo(id.toString());
        assertThat(dto.title()).isEqualTo("OCR Request");
        assertThat(dto.originalFilename()).isEqualTo("request.pdf");
        assertThat(dto.contentType()).isEqualTo("application/pdf");
        assertThat(dto.fileSize()).isEqualTo(3000L);
        assertThat(dto.fileBucket()).isEqualTo("ocr-bucket");
        assertThat(dto.fileObjectKey()).isEqualTo("ocr-key");
    }

    @Test
    void testToOcrRequestDto_nullDocument() {
        // When
        OcrRequestDto dto = mapper.toOcrRequestDto(null);

        // Then
        assertThat(dto).isNull();
    }

    // ========== toGenAIRequestDto Tests ==========

    @Test
    void testToGenAIRequestDto_withOcrText() {
        // Given
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .id(id)
                .title("GenAI Request")
                .originalFilename("genai.pdf")
                .contentType("application/pdf")
                .fileSize(4000L)
                .fileBucket("genai-bucket")
                .fileObjectKey("genai-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.OCR_COMPLETED)
                .build();

        String ocrText = "This is the OCR extracted text";

        // When
        GenAIRequestDto dto = mapper.toGenAIRequestDto(document, ocrText);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.documentId()).isEqualTo(id.toString());
        assertThat(dto.ocrText()).isEqualTo("This is the OCR extracted text");
    }

    @Test
    void testToGenAIRequestDto_withEmptyOcrText() {
        // Given
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .id(id)
                .title("GenAI Empty")
                .originalFilename("empty.pdf")
                .contentType("application/pdf")
                .fileSize(1000L)
                .fileBucket("bucket")
                .fileObjectKey("key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.OCR_COMPLETED)
                .build();

        // When
        GenAIRequestDto dto = mapper.toGenAIRequestDto(document, "");

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.documentId()).isEqualTo(id.toString());
        assertThat(dto.ocrText()).isEmpty();
    }

    @Test
    void testToGenAIRequestDto_withNullOcrText() {
        // Given
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .id(id)
                .title("GenAI Null")
                .originalFilename("null.pdf")
                .contentType("application/pdf")
                .fileSize(1000L)
                .fileBucket("bucket")
                .fileObjectKey("key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.OCR_COMPLETED)
                .build();

        // When
        GenAIRequestDto dto = mapper.toGenAIRequestDto(document, null);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.documentId()).isEqualTo(id.toString());
        assertThat(dto.ocrText()).isNull();
    }

    // ========== toSearchDocument Tests ==========

    @Test
    void testToSearchDocument_allFields() {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Document document = Document.builder()
                .id(id)
                .title("Search Index")
                .originalFilename("index.pdf")
                .contentType("application/pdf")
                .fileSize(6000L)
                .fileBucket("search-bucket")
                .fileObjectKey("search-key")
                .ocrText("Searchable text")
                .summaryText("Searchable summary")
                .createdAt(now)
                .processingStatus(Document.ProcessingStatus.COMPLETED)
                .build();

        // When
        SearchDocument searchDoc = mapper.toSearchDocument(document);

        // Then
        assertThat(searchDoc).isNotNull();
        assertThat(searchDoc.id().toString()).isEqualTo(id.toString());
        assertThat(searchDoc.title()).isEqualTo("Search Index");
        assertThat(searchDoc.originalFilename()).isEqualTo("index.pdf");
        assertThat(searchDoc.contentType()).isEqualTo("application/pdf");
        assertThat(searchDoc.fileSize()).isEqualTo(6000L);
        assertThat(searchDoc.ocrText()).isEqualTo("Searchable text");
        assertThat(searchDoc.summaryText()).isEqualTo("Searchable summary");
        assertThat(searchDoc.createdAt()).isEqualTo(now);
        assertThat(searchDoc.processingStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void testToSearchDocument_processingStatusConversion() {
        // Test all processing statuses are converted to string
        for (Document.ProcessingStatus status : Document.ProcessingStatus.values()) {
            // Given
            Document document = Document.builder()
                    .id(UUID.randomUUID())
                    .title("Status Test")
                    .originalFilename("status.pdf")
                    .contentType("application/pdf")
                    .fileSize(1000L)
                    .fileBucket("bucket")
                    .fileObjectKey("key")
                    .createdAt(Instant.now())
                    .processingStatus(status)
                    .build();

            // When
            SearchDocument searchDoc = mapper.toSearchDocument(document);

            // Then
            assertThat(searchDoc.processingStatus()).isEqualTo(status.name());
        }
    }

    @Test
    void testToSearchDocument_nullDocument() {
        // When
        SearchDocument searchDoc = mapper.toSearchDocument(null);

        // Then
        assertThat(searchDoc).isNull();
    }
}
