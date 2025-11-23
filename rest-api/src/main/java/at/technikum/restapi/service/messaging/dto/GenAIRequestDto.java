package at.technikum.restapi.service.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record GenAIRequestDto(
        @JsonProperty("document_id") String documentId,
        @JsonProperty("ocr_text") String ocrText) {
}
