package at.technikum.restapi.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import at.technikum.restapi.service.DocumentDto;
import at.technikum.restapi.service.DocumentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@RequestMapping("/documents")
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping
    public ResponseEntity<DocumentDto> upload(@RequestBody final DocumentDto doc) {
        final var savedDto = service.upload(doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> getAll() {
        final List<DocumentDto> documents = service.getAll();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getById(@PathVariable final UUID id) {
        try {
            final DocumentDto document = service.getById(id);
            return ResponseEntity.ok(document);
        } catch (final EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDto> update(@PathVariable final UUID id, @RequestBody final DocumentDto updateDoc) {
        try {
            final DocumentDto updatedDocument = service.update(id, updateDoc);
            return ResponseEntity.ok(updatedDocument);
        } catch (final EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (final IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (final EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
