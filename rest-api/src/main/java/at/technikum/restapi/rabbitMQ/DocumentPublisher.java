package at.technikum.restapi.rabbitMQ;

import at.technikum.restapi.service.DocumentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static at.technikum.restapi.rabbitMQ.RabbitConfig.*;

@Component
@RequiredArgsConstructor
public class DocumentPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishDocumentCreated(DocumentDto doc) {
        rabbitTemplate.convertAndSend(EXCHANGE, "documents.created", doc);
    }

    public void publishDocumentUpdated(DocumentDto doc) {
        rabbitTemplate.convertAndSend(EXCHANGE, "documents.updated", doc);
    }

    public void publishDocumentDeleted(String id) {
        rabbitTemplate.convertAndSend(EXCHANGE, "documents.deleted", id);
    }
}