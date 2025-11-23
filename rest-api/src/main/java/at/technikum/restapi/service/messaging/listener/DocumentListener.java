package at.technikum.restapi.service.messaging.listener;

import at.technikum.restapi.service.messaging.dto.GenAIResponseDto;
import at.technikum.restapi.service.messaging.dto.OcrResponseDto;

public interface DocumentListener {
    void handleOcrResponse(final OcrResponseDto response);

    void handleGenAIResponse(final GenAIResponseDto response);
}
