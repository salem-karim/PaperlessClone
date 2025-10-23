package at.technikum.restapi.service.dto;

import java.time.Instant;
import java.util.UUID;

import at.technikum.restapi.persistence.Document.OcrStatus;
import lombok.Builder;

@Builder
public record DocumentSummaryDto(
        UUID id,
        String title,
        String originalFilename,
        Long fileSize,
        String contentType,
        OcrStatus ocrStatus,
        Instant createdAt
) {
}
