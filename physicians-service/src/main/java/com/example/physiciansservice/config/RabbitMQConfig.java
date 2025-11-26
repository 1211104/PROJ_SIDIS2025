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

    // Cria a Exchange do tipo Fanout (Broadcast) automaticamente
    @Bean
    public FanoutExchange physicianExchange() {
        return new FanoutExchange(exchangeName);
    }

    // A fila única para cada instância
    // Cada vez que uma instância (A ou B) arranca, cria uma fila nova só para si
    @Bean
    public Queue syncQueue() {
        return new AnonymousQueue();
    }

    // Liga a fila única desta instância à Exchange Fanout
    @Bean
    public Binding binding(FanoutExchange exchange, Queue syncQueue) {
        return BindingBuilder.bind(syncQueue).to(exchange);
    }

    // Conversor para enviar JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Template configurado com o conversor JSON
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
