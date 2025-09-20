package at.technikum.restapi;

import org.springframework.data.jpa.repository.JpaRepository;

public interface Document_Repository extends JpaRepository<Document_Entity, Long> {
}
