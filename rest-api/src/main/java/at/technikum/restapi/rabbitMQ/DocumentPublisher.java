package at.technikum.restapi.rabbitMQ;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import at.technikum.restapi.service.dto.DocumentSummaryDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitConfig rabbitConfig;

    public void publishDocumentCreated(final DocumentSummaryDto doc) {
        log.info("Publishing document created event for: {}", doc.title());
        rabbitTemplate.convertAndSend(
                rabbitConfig.getExchange(),
                rabbitConfig.getOcrRoutingKeyRequest(),
                doc);
    }
}
