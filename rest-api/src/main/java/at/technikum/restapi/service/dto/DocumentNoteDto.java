package at.technikum.restapi.service.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record DocumentNoteDto(
        UUID id,
        UUID documentId,
        String text,
        Instant createdAt
) {
}
