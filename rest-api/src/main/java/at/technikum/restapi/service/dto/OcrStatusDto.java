package at.technikum.restapi.service.dto;

import java.util.UUID;
import at.technikum.restapi.persistence.Document.ProcessingStatus;

public record OcrStatusDto(
        UUID id,
        ProcessingStatus processingStatus,
        String processingError) {
}
