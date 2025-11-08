package at.technikum.restapi.rabbitMQ;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import at.technikum.restapi.service.DocumentService;
import at.technikum.restapi.service.dto.GenAIResponseDto;
import at.technikum.restapi.service.dto.OcrResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentListenerImpl implements DocumentListener {

    private final DocumentService documentService;
    private final DocumentPublisher documentPublisher;

    @RabbitListener(queues = "#{rabbitConfig.ocrResponseQueue}")
    public void handleOcrResponse(final OcrResponseDto response) {
        log.info("Received OCR response for document: {}", response.documentId());
        log.info("Status: {}, Worker: {}", response.status(), response.worker());

        try {
            final UUID documentId = UUID.fromString(response.documentId());

            if ("completed".equals(response.status())) {
                final String ocrText = response.ocrText();
                final String ocrTextObjectKey = response.ocrTextObjectKey();
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

            } else if ("failed".equals(response.status())) {
                final String error = response.error() != null ? response.error() : "Unknown error";
                log.error("OCR processing failed for document {}: {}", documentId, error);

                documentService.markOcrAsFailed(documentId, error);

            } else {
                log.warn("Unknown OCR response status: {}", response.status());
            }

        } catch (final IllegalArgumentException e) {
            log.error("Invalid document ID in OCR response: {}", response.documentId(), e);
        } catch (final Exception e) {
            log.error("Error processing OCR response for document {}: {}",
                    response.documentId(), e.getMessage(), e);
        }
    }

    @Override
    @RabbitListener(queues = "#{rabbitConfig.genaiResponseQueue}")
    public void handleGenAIResponse(final GenAIResponseDto response) {
        log.info("Received GenAI response for document: {}", response.documentId());
        log.info("Status: {}, Worker: {}", response.status(), response.worker());

        try {
            final UUID documentId = UUID.fromString(response.documentId());

            if ("completed".equals(response.status())) {
                final String summaryText = response.summaryText();
                
                if (summaryText != null && !summaryText.isEmpty()) {
                    log.info("GenAI completed successfully with summary ({} chars)", summaryText.length());
                    log.debug("Summary preview: {}",
                            summaryText.length() > 100 ? summaryText.substring(0, 100) + "..." : summaryText);

                    documentService.updateGenAIResult(documentId, summaryText);
                } else {
                    log.warn("GenAI completed but no summary text provided");
                    documentService.markGenAIAsFailed(documentId, "No summary text generated");
                }

            } else if ("failed".equals(response.status())) {
                final String error = response.error() != null ? response.error() : "Unknown error";
                log.error("GenAI processing failed for document {}: {}", documentId, error);

                documentService.markGenAIAsFailed(documentId, error);

            } else {
                log.warn("Unknown GenAI response status: {}", response.status());
            }

        } catch (final IllegalArgumentException e) {
            log.error("Invalid document ID in GenAI response: {}", response.documentId(), e);
        } catch (final Exception e) {
            log.error("Error processing GenAI response for document {}: {}",
                    response.documentId(), e.getMessage(), e);
        }
    }
}
