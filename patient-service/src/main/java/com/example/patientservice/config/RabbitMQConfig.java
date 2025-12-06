package com.example.patientservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${hap.rabbitmq.exchange.patients}")
    private String exchangeName;

    @Value("${INSTANCE_ID}")
    private String instanceId;

    @Bean
    public FanoutExchange patientExchange() {
        return new FanoutExchange(exchangeName);
    }

    // Fila anonima para sincronizacao entre replicas
    @Bean
    public Queue syncQueue() {

        String queueName = "patient-sync-queue-" + instanceId;

        return new Queue(queueName, true);
    }

    @Bean
    public Binding binding(FanoutExchange exchange, Queue syncQueue) {
        return BindingBuilder.bind(syncQueue).to(exchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
