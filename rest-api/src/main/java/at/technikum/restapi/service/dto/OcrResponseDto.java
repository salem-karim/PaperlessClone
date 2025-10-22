package at.technikum.restapi.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponseDto {
    private String document_id;
    private String status;
    private String worker;
    private String ocr_text;
    private String ocr_text_path;
    private String error;
}
