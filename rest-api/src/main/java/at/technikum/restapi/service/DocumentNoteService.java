package at.technikum.restapi.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.DocumentNote;
import at.technikum.restapi.persistence.repository.DocumentNoteRepository;
import at.technikum.restapi.persistence.repository.DocumentRepository;
import at.technikum.restapi.service.dto.DocumentNoteDto;
import at.technikum.restapi.service.exception.DocumentNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentNoteService {

    private final DocumentRepository documentRepository;
    private final DocumentNoteRepository noteRepository;

    @Transactional
    public DocumentNoteDto addNote(final UUID documentId, final String text) {
        final Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        final DocumentNote note = DocumentNote.builder()
                .document(document)
                .text(text)
                .createdAt(Instant.now())
                .build();

        final var saved = noteRepository.save(note);

        return DocumentNoteDto.builder()
                .id(saved.getId())
                .documentId(documentId)
                .text(saved.getText())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DocumentNoteDto> listNotes(final UUID documentId) {
        return noteRepository.findByDocumentIdOrderByCreatedAtAsc(documentId)
                .stream()
                .map(n -> DocumentNoteDto.builder()
                        .id(n.getId())
                        .documentId(documentId)
                        .text(n.getText())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }
}
