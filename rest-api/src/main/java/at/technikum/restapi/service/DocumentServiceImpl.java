package at.technikum.restapi.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.repository.DocumentRepository;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.dto.OcrStatusDto;
import at.technikum.restapi.service.exception.DocumentNotFoundException;
import at.technikum.restapi.service.exception.DocumentProcessingException;
import at.technikum.restapi.service.exception.InvalidDocumentException;
import at.technikum.restapi.service.mapper.DocumentMapper;
import at.technikum.restapi.service.messaging.publisher.DocumentPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final DocumentPublisher publisher;
    private final MinioService minioService;
    private final DocumentSearchService documentSearchService;

    // Supported file types for OCR
    private static final List<String> SUPPORTED_MIME_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/tiff",
            "image/bmp",
            "image/gif");

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".pdf",
            ".png",
            ".jpg",
            ".jpeg",
            ".tiff",
            ".tif",
            ".bmp",
            ".gif");

    @Override
    public DocumentSummaryDto upload(final MultipartFile file, final String title, final Instant createdAt) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("No file provided");
        }

        // Get file information
        final String contentType = file.getContentType();
        final String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new InvalidDocumentException("Invalid filename");
        }

        // Check file extension
        final String extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new InvalidDocumentException(
                    "Unsupported file type. Supported formats: PDF, PNG, JPG, JPEG, TIFF, BMP, GIF");
        }

        // Check MIME type
        if (contentType == null || !SUPPORTED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidDocumentException(
                    "Unsupported content type: " + contentType
                            + ". Supported formats: PDF, PNG, JPG, JPEG, TIFF, BMP, GIF");
        }

        try {
            final String objectKey = minioService.uploadFile(file);

            final var entity = Document.builder()
                    .title(title)
                    .fileBucket("paperless-documents")
                    .fileObjectKey(objectKey)
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .createdAt(createdAt)
                    .fileSize(file.getSize())
                    .processingStatus(Document.ProcessingStatus.PENDING)
                    .build();

            // Save to PostgreSQL
            final var saved = repository.save(entity);

            // Index metadata in ElasticSearch immediately (search while still processing)
            documentSearchService.indexDocumentMetadata(saved);

            final var dto = mapper.toSummaryDto(saved);

            // Publish OCR request
            publisher.publishDocumentForOcr(saved);

            return dto;
        } catch (final Exception e) {
            throw new DocumentProcessingException("Error uploading document: " + title, e);
        }
    }

    @Override
    public List<DocumentSummaryDto> getAll() {
        try {
            return repository.findAll().stream().map(mapper::toSummaryDto).toList();
        } catch (final DataAccessException e) {
            log.error("Failed to fetch documents: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Error fetching documents", e);
        }
    }

    @Override
    public DocumentDetailDto getById(final UUID id) {
        try {
            final var entity = repository.findById(id)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            return mapper.toDetailDto(entity);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error accessing document with ID=" + id, e);
        }
    }

    @Override
    public OcrStatusDto getOcrStatus(final UUID id) {
        try {
            final var entity = repository.findById(id)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            return mapper.toOcrStatusDto(entity);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error fetching OCR status for ID=" + id, e);
        }
    }

    @Override
    public DocumentSummaryDto update(final UUID id, final DocumentSummaryDto updateDoc) {
        if (updateDoc.id() != null && !updateDoc.id().equals(id)) {
            throw new InvalidDocumentException("ID in path does not match ID in body");
        }

        try {
            var entity = repository.findById(id)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            if (updateDoc.title() != null) {
                entity.setTitle(updateDoc.title());
            }

            entity = repository.save(entity);

            // Update metadata in ElasticSearch
            documentSearchService.updateDocumentStatus(entity);

            return mapper.toSummaryDto(entity);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error updating document with ID=" + id, e);
        }
    }

    @Override
    public void delete(final UUID id) {
        try {
            final var document = repository.findById(id)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            // Delete document file from MinIO
            minioService.deleteFile(document.getFileObjectKey());

            // Delete from ElasticSearch
            documentSearchService.deleteFromIndex(id);

            // Delete from PostgreSQL
            repository.deleteById(id);

            log.info("Document with ID='{}' successfully deleted", id);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error deleting document with ID=" + id, e);
        }
    }

    @Override
    public void updateOcrResult(final UUID documentId, final String ocrText, final String ocrTextObjectKey) {
        try {
            final var document = repository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));

            document.setProcessingStatus(Document.ProcessingStatus.OCR_COMPLETED);
            document.setOcrProcessedAt(Instant.now());

            // Handle both inline and MinIO-stored text
            if (ocrText != null) {
                // Small text sent inline
                document.setOcrText(ocrText);
                log.info("Updated document {} with inline OCR text ({} chars)",
                        documentId, ocrText.length());
            } else if (ocrTextObjectKey != null) {
                // Large text in MinIO - fetch, save, then DELETE
                try {
                    final String largeOcrText = minioService.downloadOcrText(ocrTextObjectKey);
                    document.setOcrText(largeOcrText);
                    log.info("Updated document {} with OCR text from MinIO ({} chars)",
                            documentId, largeOcrText.length());

                    // IMPORTANT: Delete from MinIO immediately after saving to PostgreSQL
                    minioService.deleteOcrText(ocrTextObjectKey);
                    log.info("Deleted temporary OCR text from MinIO: {}", ocrTextObjectKey);
                } catch (final Exception e) {
                    log.error("Failed to fetch/delete OCR text from MinIO: {}", e.getMessage());
                    throw new DocumentProcessingException("Error processing MinIO OCR text", e);
                }
            } else {
                throw new DocumentProcessingException("OCR result missing both inline text and MinIO reference");
            }

            // Save to PostgreSQL
            final var saved = repository.save(document);
            log.info("Document {} OCR processing completed", documentId);

            // Update in ElasticSearch with OCR text
            documentSearchService.updateDocumentAfterOcr(saved);

            // Publish to GenAI queue with TRUNCATED text (to avoid RabbitMQ size limits)
            publisher.publishDocumentForGenAI(saved);
        } catch (final DocumentNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to update OCR result for document {}: {}", documentId, e.getMessage());
            throw new DocumentProcessingException("Error updating OCR result", e);
        }
    }

    @Override
    public void markOcrAsFailed(final UUID documentId, final String error) {
        try {
            final var document = repository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));

            document.setProcessingStatus(Document.ProcessingStatus.OCR_FAILED);
            document.setProcessingError(error);

            final var saved = repository.save(document);

            // Update status in ElasticSearch
            documentSearchService.updateDocumentStatus(saved);

            log.error("Marked document {} as OCR_FAILED: {}", documentId, error);
        } catch (final DocumentNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to mark document {} as failed: {}", documentId, e.getMessage());
            throw new DocumentProcessingException("Error marking document as failed: " + documentId, e);
        }
    }

    @Override
    public void updateGenAIResult(final UUID documentId, final String summaryText) {
        try {
            final var document = repository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));

            document.setProcessingStatus(Document.ProcessingStatus.COMPLETED);
            document.setSummaryText(summaryText);
            document.setGenaiProcessedAt(Instant.now());

            final var saved = repository.save(document);

            // Update in ElasticSearch with summary text
            documentSearchService.updateDocumentAfterGenAI(saved);

            log.info("Document {} GenAI processing completed ({} chars summary)",
                    documentId, summaryText != null ? summaryText.length() : 0);
        } catch (final DocumentNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to update GenAI result for document {}: {}", documentId, e.getMessage());
            throw new DocumentProcessingException("Error updating GenAI result for document: " + documentId, e);
        }
    }

    @Override
    public void markGenAIAsFailed(final UUID documentId, final String error) {
        try {
            final var document = repository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));

            document.setProcessingStatus(Document.ProcessingStatus.GENAI_FAILED);
            document.setProcessingError(error);

            final var saved = repository.save(document);

            // Update status in ElasticSearch
            documentSearchService.updateDocumentStatus(saved);

            log.error("Marked document {} as GENAI_FAILED: {}", documentId, error);
        } catch (final DocumentNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to mark document {} GenAI as failed: {}", documentId, e.getMessage());
            throw new DocumentProcessingException("Error marking GenAI as failed: " + documentId, e);
        }
    }

    @Override
    public List<DocumentSummaryDto> search(final String query) {
        try {
            // Delegate to search service
            final var searchResults = documentSearchService.search(query);

            // Map SearchDocuments to DTOs
            return searchResults.stream()
                    .map(mapper::toSummaryDto)
                    .toList();
        } catch (final Exception e) {
            log.error("Failed to search documents for query '{}': {}", query, e.getMessage(), e);
            throw new DocumentProcessingException("Error searching documents for query: " + query, e);
        }
    }
}
