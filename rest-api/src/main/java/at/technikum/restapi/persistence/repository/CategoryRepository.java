package at.technikum.restapi.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import at.technikum.restapi.persistence.model.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
