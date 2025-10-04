package at.technikum.restapi.rabbitMQ;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    
    // Exchange for document operations
    public static final String EXCHANGE = "documents.operations";
    
    // Queue names for different operations
    public static final String DOCUMENTS_QUEUE = "documents.processing";
    
    @Bean
    public TopicExchange documentsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue documentsQueue() {
        return new Queue(DOCUMENTS_QUEUE);
    }

    @Bean
    public Binding documentsBinding(Queue documentsQueue, TopicExchange documentsExchange) {
        // Bind to all document operations
        return BindingBuilder
            .bind(documentsQueue)
            .to(documentsExchange)
            .with("documents.*");
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        return connectionFactory;
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}