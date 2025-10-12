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

@Configuration
public class RabbitConfig {

    // Exchange for document operations
    public static final String EXCHANGE = "documents.operations";

    // Queue names for different operations
    public static final String DOCUMENTS_QUEUE = "documents.processing";

    @Value("${RABBITMQ_HOST:localhost}")
    private String rabbitHost;

    @Bean
    TopicExchange documentsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue documentsQueue() {
        return new Queue(DOCUMENTS_QUEUE, true);
    }

    @Bean
    Binding documentsBinding(final Queue documentsQueue, final TopicExchange documentsExchange) {
        // Bind to all document operations
        return BindingBuilder
                .bind(documentsQueue)
                .to(documentsExchange)
                .with("documents.*");
    }

    @Bean
    ConnectionFactory connectionFactory() {
        final var connectionFactory = new CachingConnectionFactory(rabbitHost);
        return connectionFactory;
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(final Jackson2JsonMessageConverter messageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
