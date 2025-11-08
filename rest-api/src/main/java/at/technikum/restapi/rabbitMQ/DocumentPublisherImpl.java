package at.technikum.restapi.rabbitMQ;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import at.technikum.restapi.persistence.Document;
import at.technikum.restapi.service.dto.OcrRequestDto;
import at.technikum.restapi.service.dto.OcrResponseDto;
import at.technikum.restapi.service.mapper.DocumentMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPublisherImpl implements DocumentPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitConfig rabbitConfig;
    private final DocumentMapper mapper;

    public void publishDocumentForOcr(final Document document) {
        log.info("Publishing OCR request for document: {} (ID: {})",
                document.getTitle(), document.getId());

        // Use mapper to convert entity to OCR request DTO
        final OcrRequestDto ocrRequest = mapper.toOcrRequestDto(document);

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

        // Use mapper to convert entity to GenAI request DTO
        final var genaiRequest = mapper.toGenAIRequestDto(document);

        log.debug("GenAI Request payload: {}", genaiRequest);

        rabbitTemplate.convertAndSend(
                rabbitConfig.getExchange(),
                rabbitConfig.getGenaiRoutingKeyRequest(),
                genaiRequest);

        log.info("Published GenAI request for document ID: {}", document.getId());
    }
}
