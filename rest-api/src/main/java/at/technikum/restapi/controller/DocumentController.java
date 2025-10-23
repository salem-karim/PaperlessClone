package at.technikum.restapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import at.technikum.restapi.service.DocumentService;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.dto.OcrStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentSummaryDto> uploadDocument(
            @RequestParam("file") final MultipartFile file,
            @RequestParam("title") final String title,
            @RequestParam("createdAt") final Long createdAtMillis) {
        log.info("Received upload request: Title={}", title);
        final var savedDto = service.upload(file, title, Instant.ofEpochMilli(createdAtMillis));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryDto>> getAll() {
        log.debug("Fetching all documents");
        final var documents = service.getAll();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailDto> getById(@PathVariable final UUID id) {
        log.debug("Fetching document with ID={}", id);
        final var document = service.getById(id);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<OcrStatusDto> getOcrStatus(@PathVariable final UUID id) {
        log.debug("Fetching OCR status for document ID={}", id);
        final var statusDto = service.getOcrStatus(id);
        return ResponseEntity.ok(statusDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentSummaryDto> update(
            @PathVariable final UUID id,
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
