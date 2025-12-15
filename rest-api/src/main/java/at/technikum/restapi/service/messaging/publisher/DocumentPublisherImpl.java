package at.technikum.restapi.service.messaging.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import at.technikum.restapi.config.RabbitConfig;
import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.service.mapper.DocumentMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPublisherImpl implements DocumentPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitConfig rabbitConfig;
    private final DocumentMapper mapper;

    @Value("${SUMMARY_MAX_INPUT_LENGTH:300000}")
    private int SUMMARY_MAX_INPUT_LENGTH;

    @Override
    public void publishDocumentForOcr(final Document document) {
        log.info("Publishing OCR request for document: {} (ID: {})",
                document.getTitle(), document.getId());

        // Use mapper to convert entity to OCR request DTO
        final var ocrRequest = mapper.toOcrRequestDto(document);

        log.debug("OCR Request payload: {}", ocrRequest);

        rabbitTemplate.convertAndSend(
                rabbitConfig.getExchange(),
                rabbitConfig.getOcrRoutingKeyRequest(),
                ocrRequest);

        log.info("Published OCR request for document ID: {}", document.getId());
    }

    @Override
    public void publishDocumentForGenAI(final Document document) {
        log.info("Publishing GenAI request for document: {} (ID: {})",
                document.getTitle(), document.getId());

        String ocrText = document.getOcrText();
        if (ocrText == null || ocrText.isBlank()) {
            log.warn("Document ID: {} has no OCR text available. GenAI request may fail.", document.getId());
            return;
        }

        // Truncate OCR text if too long for Message Queue
        if (ocrText.length() > SUMMARY_MAX_INPUT_LENGTH)
            ocrText = ocrText.substring(0, SUMMARY_MAX_INPUT_LENGTH);

        // Use mapper to convert entity to GenAI request DTO
        final var genaiRequest = mapper.toGenAIRequestDto(document, ocrText);

        log.debug("GenAI Request payload: {}", genaiRequest);

        rabbitTemplate.convertAndSend(
                rabbitConfig.getExchange(),
                rabbitConfig.getGenaiRoutingKeyRequest(),
                genaiRequest);

        log.info("Published GenAI request for document ID: {}", document.getId());
    }
}
