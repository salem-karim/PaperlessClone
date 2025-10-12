package at.technikum.restapi.rabbitMQ;

import at.technikum.restapi.service.DocumentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static at.technikum.restapi.rabbitMQ.RabbitConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishDocumentCreated(final DocumentDto doc) {
        log.info("Publishing document created event for: {}", doc.getTitle());
        rabbitTemplate.convertAndSend(EXCHANGE, "documents.ocr.request", doc);
    }
}
