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
import at.technikum.restapi.rabbitMQ.DocumentPublisher;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.exception.*;
import at.technikum.restapi.service.mapper.DocumentMapper;

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

    @Override
    public DocumentSummaryDto upload(final MultipartFile file, final String title, final Instant createdAt) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("No file provided");
        }

        // Check MIME type
        if (!"application/pdf".equals(file.getContentType())) {
            throw new InvalidDocumentException("Only PDF files are allowed");
        }

        // Optional: double-check extension
        final String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new InvalidDocumentException("File must have .pdf extension");
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
                    .ocrStatus(Document.OcrStatus.PENDING)
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

            final String downloadUrl = minioService.generatePresignedUrl(entity.getFileObjectKey(), 15);

            // Get OCR text - either inline or from MinIO
            String ocrText = entity.getOcrText();
            if (ocrText == null && entity.getOcrTextObjectKey() != null) {
                // OCR text is stored in MinIO, download it
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

            document.setOcrStatus(Document.OcrStatus.COMPLETED);

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
            repository.save(document);
            log.info("Document {} OCR status updated to COMPLETED", documentId);
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

            document.setOcrStatus(Document.OcrStatus.FAILED);
            repository.save(document);

            log.error("Marked document {} as OCR FAILED: {}", documentId, error);
        } catch (final DocumentNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to mark document {} as failed: {}", documentId, e.getMessage());
            throw new DocumentProcessingException("Error marking document as failed: " + documentId, e);
        }
    }
}
