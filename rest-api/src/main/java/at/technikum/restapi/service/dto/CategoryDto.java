package at.technikum.restapi.service.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CategoryDto(
        UUID id,
        String name,
        String color,
        String icon,
        Instant createdAt,
        Instant updatedAt) {
}
