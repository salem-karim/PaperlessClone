package at.technikum.restapi.service.dto;

import java.util.UUID;

import lombok.Builder;

@Builder
public record DocumentSummaryDto(
        UUID id,
        String title,
        String originalFilename,
        Long fileSize,
        String contentType) {
}
