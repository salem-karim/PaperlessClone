package at.technikum.restapi.service.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record OcrResponseDto(
        @JsonProperty("document_id") String documentId,
        @JsonProperty("ocr_text") String ocrText,
        @JsonProperty("ocr_text_object_key") String ocrTextObjectKey,
        String status,
        String worker,
        String error) {
}
