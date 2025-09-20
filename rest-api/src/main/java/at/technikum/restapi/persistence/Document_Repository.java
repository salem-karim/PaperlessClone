package at.technikum.restapi.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface Document_Repository extends JpaRepository<Document_Entity, Long> {
}
