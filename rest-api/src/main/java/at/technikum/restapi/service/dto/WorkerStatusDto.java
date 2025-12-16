package at.technikum.restapi.service.dto;

import java.util.UUID;

import at.technikum.restapi.persistence.model.Document.ProcessingStatus;
import lombok.Builder;

@Builder
public record WorkerStatusDto(
        UUID id,
        ProcessingStatus processingStatus,
        String processingError) {
}
