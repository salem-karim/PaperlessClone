package at.technikum.restapi.rabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class RabbitConfig {

    @Value("${EXCHANGE:documents.operations}")
    private String exchange;

    @Value("${QUEUE:documents.processing}")
    private String documentsQueue;

    @Value("${RESPONSE_QUEUE:documents.processing.response}")
    private String responseQueue;

    @Value("${OCR_ROUTING_KEY_REQUEST:documents.ocr.request}")
    private String ocrRoutingKeyRequest;

    @Value("${OCR_ROUTING_KEY_RESPONSE:documents.ocr.response}")
    private String ocrRoutingKeyResponse;

    @Value("${RABBITMQ_HOST:localhost}")
    private String rabbitHost;

    @Bean
    TopicExchange documentsExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    Queue documentsQueue() {
        return new Queue(documentsQueue, true);
    }

    @Bean
    Queue responseQueue() {
        return new Queue(responseQueue, true);
    }

    // Binding for requests (REST API -> OCR Worker)
    @Bean
    Binding requestBinding(final Queue documentsQueue, final TopicExchange documentsExchange) {
        return BindingBuilder.bind(documentsQueue).to(documentsExchange).with(ocrRoutingKeyRequest);
    }

    // Binding for responses (OCR Worker -> REST API)
    @Bean
    Binding responseBinding(final Queue responseQueue, final TopicExchange documentsExchange) {
        return BindingBuilder.bind(responseQueue).to(documentsExchange).with(ocrRoutingKeyResponse);
    }

    @Bean
    ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(rabbitHost);
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory,
            final Jackson2JsonMessageConverter messageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
