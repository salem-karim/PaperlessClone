package at.technikum.restapi.controller;

import java.util.List;
import java.util.UUID;

import at.technikum.restapi.service.DocumentNoteService;
import at.technikum.restapi.service.dto.DocumentNoteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents/{documentId}/notes")
@RequiredArgsConstructor
public class DocumentNoteController {

    private final DocumentNoteService noteService;

    @PostMapping
    public ResponseEntity<DocumentNoteDto> addNote(
            @PathVariable final UUID documentId,
            @RequestBody final String text) {

        log.info("Adding note for document {}: {}", documentId, text);
        final var created = noteService.addNote(documentId, text);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public ResponseEntity<List<DocumentNoteDto>> listNotes(
            @PathVariable final UUID documentId) {

        log.info("Listing notes for document {}", documentId);
        final var notes = noteService.listNotes(documentId);
        return ResponseEntity.ok(notes);
    }
}
