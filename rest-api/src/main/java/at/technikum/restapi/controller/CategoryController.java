package at.technikum.restapi.controller;

import org.springframework.web.bind.annotation.*;

import at.technikum.restapi.service.CategoryService;
import at.technikum.restapi.service.dto.CategoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Slf4j
@RestController
@RequestMapping("categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @PostMapping
    public ResponseEntity<CategoryDto> upload(final CategoryDto category) {
        log.info("Received upload request: Name={}", category.name());
        final var savedDto = service.upload(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAll() {
        log.debug("Fetching all Categories");
        final var categories = service.getAll();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable final UUID id) {
        log.debug("Fetching Category: ID:{}", id);
        final var category = service.getById(id);
        return ResponseEntity.ok(category);
    }

    @PutMapping
    public ResponseEntity<CategoryDto> update(@RequestParam final CategoryDto category) {
        log.info("Received update request: Name={}, ID={}", category.name(), category.id());
        final var updatedCategory = service.update(category);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam final UUID id) {
        log.info("Received delete request: ID={}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
