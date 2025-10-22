package at.technikum.restapi.rabbitMQ;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import at.technikum.restapi.service.dto.OcrResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentResponseListener {

    @RabbitListener(queues = "#{rabbitConfig.responseQueue}")
    public void handleOcrResponse(OcrResponseDto response) {
        log.info("Received OCR response for document: {}", response.getDocument_id());
        log.info("Status: {}, Worker: {}", response.getStatus(), response.getWorker());

        if ("completed".equals(response.getStatus())) {
            String ocrText = response.getOcr_text();
            log.info("OCR completed successfully");
            log.info("OCR Text preview: {}",
                    ocrText != null && ocrText.length() > 100
                            ? ocrText.substring(0, 100) + "..."
                            : ocrText);

            // TODO: Update document in database with OCR text
            // documentService.updateOcrText(UUID.fromString(response.getDocument_id()),
            // ocrText);

        } else if ("failed".equals(response.getStatus())) {
            log.error("OCR processing failed for document {}: {}",
                    response.getDocument_id(), response.getError());

            // TODO: Update document status to failed
            // documentService.markAsFailed(UUID.fromString(response.getDocument_id()),
            // response.getError());
        }
    }
}
