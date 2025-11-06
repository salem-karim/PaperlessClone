package at.technikum.restapi.rabbitMQ;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import at.technikum.restapi.service.DocumentService;
import at.technikum.restapi.service.dto.OcrResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentListenerImpl implements DocumentListener {

    private final DocumentService documentService;

    @RabbitListener(queues = "#{rabbitConfig.ocrResponseQueue}")
    public void handleOcrResponse(final OcrResponseDto response) {
        log.info("Received OCR response for document: {}", response.getDocumentId());
        log.info("Status: {}, Worker: {}", response.getStatus(), response.getWorker());

        try {
            final UUID documentId = UUID.fromString(response.getDocumentId());

            if ("completed".equals(response.getStatus())) {
                final String ocrText = response.getOcrText();
                final String ocrTextObjectKey = response.getOcrTextObjectKey();
                if (ocrText != null && !ocrText.isEmpty()) {
                    // Small text sent inline
                    log.info("OCR completed successfully with inline text ({} chars)", ocrText.length());
                    log.debug("OCR Text preview: {}",
                            ocrText.length() > 100 ? ocrText.substring(0, 100) + "..." : ocrText);

                    documentService.updateOcrResult(documentId, ocrText, null);

                } else if (ocrTextObjectKey != null && !ocrTextObjectKey.isEmpty()) {
                    // Large text stored in MinIO
                    log.info("OCR completed successfully, text stored in MinIO: {}", ocrTextObjectKey);

                    documentService.updateOcrResult(documentId, null, ocrTextObjectKey);

                } else {
                    log.warn("OCR completed but no text or object key provided");
                    documentService.markOcrAsFailed(documentId, "No OCR text or reference provided");
                }

            } else if ("failed".equals(response.getStatus())) {
                final String error = response.getError() != null ? response.getError() : "Unknown error";
                log.error("OCR processing failed for document {}: {}", documentId, error);

                documentService.markOcrAsFailed(documentId, error);

            } else {
                log.warn("Unknown OCR response status: {}", response.getStatus());
            }

        } catch (final IllegalArgumentException e) {
            log.error("Invalid document ID in OCR response: {}", response.getDocumentId(), e);
        } catch (final Exception e) {
            log.error("Error processing OCR response for document {}: {}",
                    response.getDocumentId(), e.getMessage(), e);
        }
    }
}
