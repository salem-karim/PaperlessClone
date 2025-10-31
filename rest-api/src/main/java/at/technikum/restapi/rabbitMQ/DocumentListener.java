package at.technikum.restapi.rabbitMQ;

import at.technikum.restapi.service.dto.OcrResponseDto;
// import at.technikum.restapi.service.dto.GenAIResponseDto;

public interface DocumentListener {
    void handleOcrResponse(final OcrResponseDto response);

    // void handleGenAIResponse(final GenAIResponseDto response);
}
