package at.technikum.restapi.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record OcrRequestDto(
        @JsonProperty("document_id")
        String documentId,
        
        String title,
        
        @JsonProperty("original_filename")
        String originalFilename,
        
        @JsonProperty("content_type")
        String contentType,
        
        @JsonProperty("file_size")
        Long fileSize,
        
        @JsonProperty("file_bucket")
        String fileBucket,
        
        @JsonProperty("file_object_key")
        String fileObjectKey
) {
}