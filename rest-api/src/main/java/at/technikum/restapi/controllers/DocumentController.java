package at.technikum.restapi.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import at.technikum.restapi.persistence.DocumentEntity;
import at.technikum.restapi.persistence.DocumentRepository;

import java.util.List;
import java.util.UUID;

// REST API Controller
@RequestMapping("/documents")
@RestController
public class DocumentController {

    private final DocumentRepository repository;

    public DocumentController(DocumentRepository repository) {
        this.repository = repository;
    }

    // Upload (create)
    @PostMapping
    public DocumentEntity upload(@RequestBody DocumentEntity doc) {
        return repository.save(doc);
    }

    // Get all documents
    @GetMapping
    public List<DocumentEntity> getAll() {
        return repository.findAll();
    }

    // Get document by ID
    @GetMapping("/{id}")
    public ResponseEntity<DocumentEntity> getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update document
    @PutMapping("/{id}")
    public ResponseEntity<DocumentEntity> update(@PathVariable UUID id, @RequestBody DocumentEntity newDoc) {
        return repository.findById(id)
                .map(doc -> {
                    doc.setTitle(newDoc.getTitle());
                    return ResponseEntity.ok(repository.save(doc));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
