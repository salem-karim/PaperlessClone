package at.technikum.restapi.config;

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

    @Value("${OCR_QUEUE:documents.ocr.processing}")
    private String ocrQueue;

    @Value("${OCR_RESPONSE_QUEUE:documents.ocr.processing.response}")
    private String ocrResponseQueue;

    @Value("${GENAI_QUEUE:documents.genai.processing}")
    private String genaiQueue;

    @Value("${GENAI_RESPONSE_QUEUE:documents.genai.processing.response}")
    private String genaiResponseQueue;

    @Value("${OCR_ROUTING_KEY_REQUEST:documents.ocr.request}")
    private String ocrRoutingKeyRequest;

    @Value("${OCR_ROUTING_KEY_RESPONSE:documents.ocr.response}")
    private String ocrRoutingKeyResponse;

    @Value("${GENAI_ROUTING_KEY_REQUEST:documents.genai.request}")
    private String genaiRoutingKeyRequest;

    @Value("${GENAI_ROUTING_KEY_RESPONSE:documents.genai.response}")
    private String genaiRoutingKeyResponse;

    @Value("${RABBITMQ_HOST:localhost}")
    private String rabbitHost;

    @Bean
    TopicExchange documentsExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    Queue ocrQueue() {
        return new Queue(ocrQueue, true);
    }

    @Bean
    Queue ocrResponseQueue() {
        return new Queue(ocrResponseQueue, true);
    }

    @Bean
    Queue genaiQueue() {
        return new Queue(genaiQueue, true);
    }

    @Bean
    Queue genaiResponseQueue() {
        return new Queue(genaiResponseQueue, true);
    }

    // Binding for OCR requests (REST API -> OCR Worker)
    @Bean
    Binding ocrRequestBinding(final Queue ocrQueue, final TopicExchange documentsExchange) {
        return BindingBuilder.bind(ocrQueue).to(documentsExchange).with(ocrRoutingKeyRequest);
    }

    // Binding for OCR responses (OCR Worker -> REST API)
    @Bean
    Binding ocrResponseBinding(final Queue ocrResponseQueue, final TopicExchange documentsExchange) {
        return BindingBuilder.bind(ocrResponseQueue).to(documentsExchange).with(ocrRoutingKeyResponse);
    }

    // Binding for GenAI requests (REST API -> GenAI Worker)
    @Bean
    Binding genaiRequestBinding(final Queue genaiQueue, final TopicExchange documentsExchange) {
        return BindingBuilder.bind(genaiQueue).to(documentsExchange).with(genaiRoutingKeyRequest);
    }

    // Binding for GenAI responses (GenAI Worker -> REST API)
    @Bean
    Binding genaiResponseBinding(final Queue genaiResponseQueue, final TopicExchange documentsExchange) {
        return BindingBuilder.bind(genaiResponseQueue).to(documentsExchange).with(genaiRoutingKeyResponse);
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
