package at.technikum.restapi.service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import at.technikum.restapi.persistence.DocumentEntity;
import at.technikum.restapi.persistence.DocumentRepository;
import at.technikum.restapi.rabbitMQ.DocumentPublisher;
import at.technikum.restapi.service.exceptions.*;
import at.technikum.restapi.service.mapper.DocumentMapper;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.BucketExistsArgs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final DocumentPublisher publisher;
    private final MinioClient minioClient;

    private static final String BUCKET_NAME = "documents";

    /**
     * Upload a real file (with title) to MinIO + save metadata + publish OCR event
     */
    @Override
    public DocumentDto upload(final MultipartFile file, final String title) {
        try {
            // Step 1: Ensure bucket exists
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(BUCKET_NAME).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(BUCKET_NAME).build()
                );
                log.info("Created MinIO bucket '{}'", BUCKET_NAME);
            }

            // Step 2: Upload file to MinIO
            String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();

            try (InputStream input = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .object(objectKey)
                                .stream(input, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            // Step 3: Save document metadata in database
            var entity = DocumentEntity.builder()
                    .title(title)
                    .objectKey(objectKey)
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .build();

            var saved = repository.save(entity);
            var dto = mapper.toDto(saved);

            // Step 4: Notify OCR worker via RabbitMQ
            publisher.publishDocumentCreated(dto);

            log.info("Document uploaded & queued for OCR: Title='{}', Key='{}'", title, objectKey);
            return dto;

        } catch (Exception e) {
            log.error("Failed to upload document '{}': {}", title, e.getMessage(), e);
            throw new DocumentProcessingException("Error uploading document: " + title, e);
        }
    }

    /**
     * ✅ Legacy method (metadata only)
     * Still available for backward compatibility.
     */
    @Override
    public DocumentDto upload(final DocumentDto doc) {
        try {
            var saved = repository.save(mapper.toEntity(doc));
            var dto = mapper.toDto(saved);
            publisher.publishDocumentCreated(dto);
            log.info("Document with Title='{}' successfully uploaded (metadata only)", doc.getTitle());
            return dto;
        } catch (final Exception e) {
            log.error("Failed to upload metadata Title='{}': {}", doc.getTitle(), e.getMessage(), e);
            throw new DocumentProcessingException("Error uploading document: " + doc.getTitle(), e);
        }
    }

    @Override
    public List<DocumentDto> getAll() {
        try {
            return repository.findAll().stream().map(mapper::toDto).toList();
        } catch (final DataAccessException e) {
            log.error("Failed to fetch documents: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Error fetching documents", e);
        }
    }

    @Override
    public DocumentDto getById(final UUID id) {
        try {
            return repository.findById(id)
                    .map(mapper::toDto)
                    .orElseThrow(() -> new DocumentNotFoundException(id));
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error accessing document with ID=" + id, e);
        }
    }

    @Override
    public DocumentDto update(final UUID id, final DocumentDto updateDoc) {
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
            return mapper.toDto(entity);
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
