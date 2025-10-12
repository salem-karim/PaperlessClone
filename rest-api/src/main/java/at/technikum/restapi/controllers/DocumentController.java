package at.technikum.restapi.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import at.technikum.restapi.service.DocumentDto;
import at.technikum.restapi.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping
    public ResponseEntity<DocumentDto> upload(@RequestBody final DocumentDto doc) {
        log.info("Received upload request: Title={}", doc.getTitle());
        final var savedDto = service.upload(doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> getAll() {
        log.debug("Fetching all documents");
        final var documents = service.getAll();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getById(@PathVariable final UUID id) {
        log.debug("Fetching document with ID={}", id);
        final var document = service.getById(id);
        return ResponseEntity.ok(document);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDto> update(
            @PathVariable final UUID id,
            @RequestBody final DocumentDto updateDoc) {
        log.info("Received update request: Title={}, ID={}", updateDoc.getTitle(), updateDoc.getId());
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
