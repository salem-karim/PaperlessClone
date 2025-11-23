package at.technikum.restapi.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import at.technikum.restapi.persistence.model.DocumentNote;

public interface DocumentNoteRepository extends JpaRepository<DocumentNote, UUID> {

    List<DocumentNote> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
}
