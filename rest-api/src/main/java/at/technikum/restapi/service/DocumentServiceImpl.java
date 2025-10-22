package at.technikum.restapi.service;

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
    public DocumentSummaryDto upload(MultipartFile file, String title) {
        try {
            String objectKey = minioService.upload(file);

            var entity = Document.builder()
                    .title(title)
                    .objectKey(objectKey)
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .build();

            var saved = repository.save(entity);
            var dto = mapper.toSummaryDto(saved);
            // publisher.publishDocumentCreated(dto);
            return dto;
        } catch (Exception e) {
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
            var entity = repository.findById(id)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            String downloadUrl = minioService.generatePresignedUrl(entity.getObjectKey(), 15);
            // String ocrText = ocrTextService.getText(entity); // optional, can be
            // empty/null
            // TODO: change this to actually get the OCR Text
            String ocrText = "Template needs change";

            return mapper.toDetailDto(entity, downloadUrl, ocrText);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error accessing document with ID=" + id, e);
        }
    }

    @Override
    public DocumentSummaryDto update(final UUID id, final DocumentSummaryDto updateDoc) {
        if (updateDoc.getId() != null && !updateDoc.getId().equals(id)) {
            throw new InvalidDocumentException("ID in path does not match ID in body");
        }

        try {
            var entity = repository.findById(id)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            if (updateDoc.getTitle() != null) {
                entity.setTitle(updateDoc.getTitle());
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
            repository.deleteById(id);
            log.info("Document with ID='{}' successfully deleted", id);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error deleting document with ID=" + id, e);
        }
    }
}
