package at.technikum.restapi.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import at.technikum.restapi.persistence.model.Document;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    @EntityGraph(attributePaths = {"categories"})
    Optional<Document> findWithCategoriesById(UUID id);
}
