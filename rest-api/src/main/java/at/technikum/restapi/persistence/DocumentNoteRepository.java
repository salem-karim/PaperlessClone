package at.technikum.restapi.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentNoteRepository extends JpaRepository<DocumentNote, UUID> {

    List<DocumentNote> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
}
