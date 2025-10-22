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
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new InvalidDocumentException("File must have .pdf extension");
        }
        try {
            final String objectKey = minioService.upload(file);

            final var entity = Document.builder()
                    .title(title)
                    .objectKey(objectKey)
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .createdAt(createdAt)
                    .fileSize(file.getSize())
                    .build();

            final var saved = repository.save(entity);
            final var dto = mapper.toSummaryDto(saved);
            // publisher.publishDocumentCreated(dto);
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

            final String downloadUrl = minioService.generatePresignedUrl(entity.getObjectKey(), 15);
            // String ocrText = ocrTextService.getText(entity); // optional, can be
            // empty/null
            // TODO: change this to actually get the OCR Text
            final String ocrText = "Template needs change";

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
            if (!repository.existsById(id)) {
                throw new DocumentNotFoundException(id);
            }

            // fetch objectKey from your document entity
            String objectKey = repository.findById(id)
                    .map(Document::getObjectKey)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            minioService.delete(objectKey);

            repository.deleteById(id);
            log.info("Document with ID='{}' successfully deleted", id);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error deleting document with ID=" + id, e);
        }
    }

}
