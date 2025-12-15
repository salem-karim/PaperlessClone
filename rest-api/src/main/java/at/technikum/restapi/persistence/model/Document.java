package at.technikum.restapi.persistence.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "documents")
public class Document {

    public enum ProcessingStatus {
        PENDING, // Not started
        OCR_PROCESSING, // OCR in progress
        OCR_COMPLETED, // OCR done, waiting for GenAI
        GENAI_PROCESSING, // Summary generation in progress
        COMPLETED, // Everything done
        OCR_FAILED, // OCR failed (stops pipeline)
        GENAI_FAILED // OCR succeeded, but GenAI failed (partial failure)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize; // bytes

    // MinIO bucket and object key for the original document file
    @Column(nullable = false)
    private String fileBucket; // e.g., "paperless-documents"

    @Column(nullable = false)
    private String fileObjectKey; // e.g., "2024/10/uuid-document.pdf"

    // Document Processing Status (tracks OCR → GenAI pipeline)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    // LAZY: Large text field - only for detail view/ElasticSearch backup
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    // LAZY: Large text field - only for detail view
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT")
    private String summaryText;

    // LAZY: Only needed when there's an error
    @Basic(fetch = FetchType.LAZY)
    @Column(length = 1000)
    private String processingError;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @Column
    private Instant ocrProcessedAt;

    @Column
    private Instant genaiProcessedAt;
}
