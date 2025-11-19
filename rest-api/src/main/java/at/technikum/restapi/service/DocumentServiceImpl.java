package at.technikum.restapi.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import at.technikum.restapi.miniIO.MinioService;
import at.technikum.restapi.persistence.Document;
import at.technikum.restapi.persistence.DocumentRepository;
import at.technikum.restapi.rabbitMQ.DocumentPublisherImpl;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.dto.OcrStatusDto;
import at.technikum.restapi.service.exception.DocumentNotFoundException;
import at.technikum.restapi.service.exception.DocumentProcessingException;
import at.technikum.restapi.service.exception.InvalidDocumentException;
import at.technikum.restapi.service.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import at.technikum.restapi.persistence.SearchDocumentRepository;
import at.technikum.restapi.service.DocumentSearchIndexService;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final DocumentPublisherImpl publisher;
    private final MinioService minioService;

    private final SearchDocumentRepository searchDocumentRepository;
    private final DocumentSearchIndexService documentSearchIndexService;

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
            final String objectKey = minioService.upload(file);

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

            final var saved = repository.save(entity);
            final var dto = mapper.toSummaryDto(saved);

            // Publish OCR request using the saved entity
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

            // Always use our API endpoint for downloads
            final String downloadUrl = "/api/v1/documents/" + id + "/download";

            // Get OCR text - either inline or from MinIO
            String ocrText = entity.getOcrText();
            if (ocrText == null && entity.getOcrTextObjectKey() != null) {
                try {
                    ocrText = minioService.downloadOcrText(entity.getOcrTextObjectKey());
                } catch (final Exception e) {
                    log.warn("Failed to download OCR text for document {}: {}", id, e.getMessage());
                    ocrText = null;
                }
            }

            return mapper.toDetailDto(entity, downloadUrl, ocrText);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error accessing document with ID=" + id, e);
        }
    }

    @Override
    public OcrStatusDto getOcrStatus(final UUID id) {
        try {
            final var entity = repository.findById(id)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            return new OcrStatusDto(
                    entity.getId(),
                    entity.getProcessingStatus(),
                    entity.getProcessingError());
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
            minioService.delete(document.getFileObjectKey());

            // Delete OCR text from MinIO if it exists
            if (document.getOcrTextObjectKey() != null) {
                try {
                    minioService.deleteOcrText(document.getOcrTextObjectKey());
                } catch (final Exception e) {
                    log.warn("Failed to delete OCR text for document {}: {}", id, e.getMessage());
                }
            }

            documentSearchIndexService.deleteFromIndex(id);

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

            // Update status to OCR_COMPLETED (ready for GenAI)
            document.setProcessingStatus(Document.ProcessingStatus.OCR_COMPLETED);
            document.setOcrProcessedAt(Instant.now());

            if (ocrText != null) {
                // Small text stored inline
                document.setOcrText(ocrText);
                document.setOcrTextObjectKey(null);
                log.info("Updated document {} with inline OCR text ({} chars)",
                        documentId, ocrText.length());
            } else if (ocrTextObjectKey != null) {
                // Large text stored in MinIO
                document.setOcrText(null);
                document.setOcrTextObjectKey(ocrTextObjectKey);
                log.info("Updated document {} with OCR text reference: {}",
                        documentId, ocrTextObjectKey);
            }

            final var saved = repository.save(document);
            log.info("Document {} status updated to OCR_COMPLETED", documentId);

            documentSearchIndexService.indexDocument(saved);

            // Publish to GenAI queue for summarization
            publisher.publishDocumentForGenAI(saved);
        } catch (final DocumentNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to update OCR result for document {}: {}", documentId, e.getMessage());
            throw new DocumentProcessingException("Error updating OCR result for document: " + documentId, e);
        }
    }

    @Override
    public void markOcrAsFailed(final UUID documentId, final String error) {
        try {
            final var document = repository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));

            document.setProcessingStatus(Document.ProcessingStatus.OCR_FAILED);
            document.setProcessingError(error);
            repository.save(document);

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
            repository.save(document);

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
            repository.save(document);

            log.error("Marked document {} as GENAI_FAILED: {}", documentId, error);
        } catch (final DocumentNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to mark document {} GenAI as failed: {}", documentId, e.getMessage());
            throw new DocumentProcessingException("Error marking GenAI as failed: " + documentId, e);
        }
    }

    @Override
    public List<DocumentSummaryDto> search(final String query, final int page, final int size) {
        try {
            final var pageable = PageRequest.of(page, size);

            final var hits = searchDocumentRepository.findByContentContainingIgnoreCase(query, pageable);

            return hits.stream()
                    .map(doc -> DocumentSummaryDto.builder()
                            .id(doc.getId())
                            .title(doc.getTitle())
                            .originalFilename(doc.getOriginalFilename())
                            .fileSize(doc.getFileSize())
                            .contentType(doc.getContentType())
                            .processingStatus(doc.getProcessingStatus())
                            .createdAt(doc.getCreatedAt())
                            .build())
                    .toList();
        } catch (final Exception e) {
            log.error("Failed to search documents for query '{}': {}", query, e.getMessage(), e);
            throw new DocumentProcessingException("Error searching documents for query: " + query, e);
        }
    }

}
