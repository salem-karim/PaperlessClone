package at.technikum.restapi.service.dto;

import java.time.Instant;
import java.util.UUID;

import at.technikum.restapi.persistence.Document.OcrStatus;
import lombok.Builder;

@Builder
public record DocumentDetailDto(
        UUID id,
        String title,
        String originalFilename,
        Long fileSize,
        String contentType,
        String fileBucket,          // Added - needed for download
        String fileObjectKey,       // Added - needed for download
        Instant createdAt,
        String downloadUrl,         // Presigned URL to download original file from MinIO
        OcrStatus ocrStatus,
        String ocrText,             // Inline OCR text if available and small
        String ocrTextDownloadUrl,  // Presigned URL if OCR text is stored in MinIO
        String ocrError,            // Error message if OCR failed
        Instant ocrProcessedAt
) {
}
