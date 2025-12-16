package at.technikum.restapi.service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import at.technikum.restapi.persistence.model.Document.ProcessingStatus;
import lombok.Builder;

@Builder
public record DocumentDetailDto(
    UUID id,
    String title,
    String originalFilename,
    Long fileSize,
    String contentType,
    String fileBucket, // Added - needed for download
    String fileObjectKey, // Added - needed for download
    Instant createdAt,
    String downloadUrl, // Presigned URL to download original file from MinIO
    ProcessingStatus processingStatus,
    String summaryText, // AI-generated summary
    String processingError, // Error message if any step failed
    Instant ocrProcessedAt,
    Instant genaiProcessedAt,
    List<CategoryDto> categories) {
}
