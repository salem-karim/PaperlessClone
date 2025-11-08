package at.technikum.restapi.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
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

    // OCR Text (inline if small, < 100KB)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;
    // MinIO reference for large OCR text (> 100KB)
    private String ocrTextBucket; // e.g., "paperless-ocr-results"

    private String ocrTextObjectKey; // e.g., "2024/10/uuid-ocr.txt"

    // GenAI Summary Text
    @Lob
    @Column(columnDefinition = "TEXT")
    private String summaryText;

    // Error message for processing failures
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
