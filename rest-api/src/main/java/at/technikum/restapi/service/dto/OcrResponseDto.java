package at.technikum.restapi.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponseDto {
    @JsonProperty("document_id")
    private String documentId;
    
    private String status;
    private String worker;
    
    @JsonProperty("ocr_text")
    private String ocrText;
    
    @JsonProperty("ocr_text_object_key")
    private String ocrTextObjectKey;
    
    private String error;
}
