package at.technikum.restapi.service.dto;

import java.util.UUID;
import at.technikum.restapi.persistence.Document.OcrStatus;

public record OcrStatusDto(
    UUID id,
    OcrStatus ocrStatus,
    String ocrError
) {}