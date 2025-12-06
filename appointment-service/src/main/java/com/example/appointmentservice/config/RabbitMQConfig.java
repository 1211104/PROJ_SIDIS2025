package com.example.appointmentservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${hap.rabbitmq.exchange.physicians}")
    private String physicianExchangeName;

    @Value("${hap.rabbitmq.exchange.patients}")
    private String patientExchangeName;

    // INJETAR O ID DA INSTANCIA (Crucial para CQRS com DB local)
    @Value("${INSTANCE_ID:default}")
    private String instanceId;

    @Bean
    public FanoutExchange physicianExchange() { return new FanoutExchange(physicianExchangeName); }

    @Bean
    public FanoutExchange patientExchange() { return new FanoutExchange(patientExchangeName); }


    @Bean
    public Queue physicianQueue() {
        return new Queue("appointment-physician-sync-queue-" + instanceId, true);
    }

    @Bean
    public Queue patientQueue() {
        return new Queue("appointment-patient-sync-queue-" + instanceId, true);
    }

    // Bindings
    @Bean
    public Binding bindPhysician(FanoutExchange physicianExchange, @Qualifier("physicianQueue") Queue q) {
        return BindingBuilder.bind(q).to(physicianExchange);
    }

    @Bean
    public Binding bindPatient(FanoutExchange patientExchange, @Qualifier("patientQueue") Queue q) {
        return BindingBuilder.bind(q).to(patientExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}