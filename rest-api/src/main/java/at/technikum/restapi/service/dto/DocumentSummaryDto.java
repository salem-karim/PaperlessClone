package at.technikum.restapi.service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import at.technikum.restapi.persistence.model.Category;
import at.technikum.restapi.persistence.model.Document.ProcessingStatus;
import lombok.Builder;

@Builder
public record DocumentSummaryDto(
        UUID id,
        String title,
        String originalFilename,
        Long fileSize,
        String contentType,
        ProcessingStatus processingStatus,
        Instant createdAt,
        List<CategoryDto> categories) {
}
