package at.technikum.Batch_Processor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DocumentDailyAccessRepository
        extends JpaRepository<DocumentDailyAccess, UUID> {

    Optional<DocumentDailyAccess> findByDocumentIdAndAccessDate(
            UUID documentId,
            LocalDate accessDate);
}
