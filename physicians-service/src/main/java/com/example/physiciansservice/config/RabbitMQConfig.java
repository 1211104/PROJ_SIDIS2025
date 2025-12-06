package com.example.physiciansservice.config;

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

    @Value("${hap.rabbitmq.exchange.physicians}")
    private String exchangeName;

    // INJETAR O ID DA INSTANCIA (replicaA, replicaB)
    @Value("${INSTANCE_ID}")
    private String instanceId;

    @Bean
    public FanoutExchange physicianExchange() {
        return new FanoutExchange(exchangeName);
    }

    @Bean
    public Queue syncQueue() {

        String queueName = "physician-sync-queue-" + instanceId;

        // Durable (true): O RabbitMQ guarda a fila no disco, mesmo que ninguem esteja ligado
        // Exclusive (false): false (para reconectar)
        // AutoDelete (false): false (nao apagar quando desligado)
        return new Queue(queueName, true, false, false);
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