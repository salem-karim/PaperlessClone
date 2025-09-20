package at.technikum.restapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST API Controller
@RequestMapping("/api/documents")
@RestController
public class Document_Controller {

    private final Document_Repository repository;

    public Document_Controller(Document_Repository repository) {
        this.repository = repository;
    }

    // Upload (create)
    @PostMapping
    public Document_Entity upload(@RequestBody Document_Entity doc) {
        return repository.save(doc);
    }

    // Get all documents
    @GetMapping
    public List<Document_Entity> getAll() {
        return repository.findAll();
    }

    // Get document by ID
    @GetMapping("/{id}")
    public ResponseEntity<Document_Entity> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update document
    @PutMapping("/{id}")
    public ResponseEntity<Document_Entity> update(@PathVariable Long id, @RequestBody Document_Entity newDoc) {
        return repository.findById(id)
                .map(doc -> {
                    doc.setTitle(newDoc.getTitle());
                    return ResponseEntity.ok(repository.save(doc));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
