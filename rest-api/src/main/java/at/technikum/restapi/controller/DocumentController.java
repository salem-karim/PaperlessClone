package at.technikum.restapi.controller;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import at.technikum.restapi.service.dto.WorkerStatusDto;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import at.technikum.restapi.service.DocumentService;
import at.technikum.restapi.service.MinioService;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;
    private final MinioService minioService;

    // File size threshold: 10MB
    private static final long STREAM_SIZE_THRESHOLD = 10 * 1024 * 1024; // 10MB

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentSummaryDto> uploadDocument(@RequestParam final MultipartFile file,
            @RequestParam final String title, @RequestParam final Long createdAt,
            @RequestParam(required = false) final List<String> categoryIds) {
        log.info("Received upload request: Title={}, Categories={}", title, categoryIds);
        final List<String> safeCategoryIds = categoryIds != null ? categoryIds : Collections.emptyList();
        final var savedDto = service.upload(file, title, Instant.ofEpochMilli(createdAt), safeCategoryIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryDto>> getAll() {
        log.debug("Fetching all documents");
        final var documents = service.getAll();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentSummaryDto>> searchDocuments(@RequestParam("q") final String query,
            @RequestParam(required = false) final List<String> categories) {
        log.info("Received document search request: q='{}'", query);
        final List<String> safeCategories = categories != null ? categories : Collections.emptyList();
        final var result = service.search(query, safeCategories);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailDto> getById(@PathVariable final UUID id) {
        log.debug("Fetching document with ID={}", id);
        final var document = service.getById(id);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<WorkerStatusDto> getOcrStatus(@PathVariable final UUID id) {
        log.debug("Fetching OCR status for document ID={}", id);
        final var statusDto = service.getWorkerStatus(id);
        return ResponseEntity.ok(statusDto);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable final UUID id) {
        log.info("Download request for document ID={}", id);

        final var document = service.getById(id);

        // If file is large, return presigned URL instead of streaming
        if (document.fileSize() > STREAM_SIZE_THRESHOLD) {
            log.info("File size {} exceeds threshold, generating presigned URL", document.fileSize());

            final String presignedUrl = minioService.generatePresignedUrl(document.fileObjectKey(), 15 // 15 minutes
                                                                                                       // expiry
            );

            return ResponseEntity.ok().body(Map.of("url", presignedUrl, "filename", document.originalFilename(),
                    "fileSize", document.fileSize(), "expiresIn", 900 // 15 minutes in seconds
            ));
        }

        // Stream small files directly
        try {
            log.info("Streaming file directly (size: {})", document.fileSize());

            final InputStream stream = minioService.downloadFile(document.fileObjectKey());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.originalFilename() + "\"")
                    .contentType(MediaType.parseMediaType(document.contentType())).contentLength(document.fileSize())
                    .body(new InputStreamResource(stream));

        } catch (final Exception e) {
            log.error("Failed to download document {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to download document", e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentSummaryDto> update(@PathVariable final UUID id,
            @RequestBody final DocumentSummaryDto updateDoc) {
        log.info("Received update request: Title={}, ID={}", updateDoc.title(), updateDoc.id());
        final var updatedDocument = service.update(id, updateDoc);
        return ResponseEntity.ok(updatedDocument);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        log.info("Received delete request: ID={}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
