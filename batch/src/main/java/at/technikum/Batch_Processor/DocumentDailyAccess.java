package at.technikum.Batch_Processor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_daily_access", uniqueConstraints = @UniqueConstraint(columnNames = { "document_id",
        "access_date" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDailyAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "access_date", nullable = false)
    private LocalDate accessDate;

    @Column(name = "access_count", nullable = false)
    private int accessCount;

    @Column(name = "source")
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
