package com.example.physiciansservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // CONFIGURAÇÃO EXISTENTE (CQRS / SYNC)
    @Value("${hap.rabbitmq.exchange.physicians}")
    private String exchangeName;

    @Value("${INSTANCE_ID}")
    private String instanceId;

    @Bean
    public FanoutExchange physicianExchange() {
        return new FanoutExchange(exchangeName);
    }

    @Bean
    public Queue syncQueue() {
        String queueName = "physician-sync-queue-" + instanceId;
        return new Queue(queueName, true, false, false);
    }

    @Bean
    public Binding binding(@Qualifier("physicianExchange") FanoutExchange exchange,
                           @Qualifier("syncQueue") Queue syncQueue) {
        return BindingBuilder.bind(syncQueue).to(exchange);
    }

    // ==========================================
    // CONFIGURAÇÃO PARA A SAGA
    // ==========================================

    @Value("${hap.rabbitmq.exchange.appointments:appointment-exchange}")
    private String appointmentExchangeName;

    @Bean
    public FanoutExchange appointmentExchange() {
        return new FanoutExchange(appointmentExchangeName);
    }

    @Bean
    public Queue sagaAppointmentQueue() {
        return new Queue("saga-physician-appointment-listener", true);
    }


    @Bean
    public Binding bindSagaQueue(@Qualifier("appointmentExchange") FanoutExchange appointmentExchange,
                                 @Qualifier("sagaAppointmentQueue") Queue q) {
        return BindingBuilder.bind(q).to(appointmentExchange);
    }

    @Bean
    public Queue responseQueue() {
        return new Queue("physician-response-queue", true);
    }

    // ==========================================
    // UTILITÁRIOS
    // ==========================================

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