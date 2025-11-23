package at.technikum.restapi.service.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record GenAIResponseDto(
        @JsonProperty("document_id") String documentId,
        @JsonProperty("summary_text") String summaryText,
        String status,
        String worker,
        String error) {
}
