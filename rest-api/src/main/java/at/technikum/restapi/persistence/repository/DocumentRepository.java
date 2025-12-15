package at.technikum.restapi.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import at.technikum.restapi.persistence.model.Document;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
