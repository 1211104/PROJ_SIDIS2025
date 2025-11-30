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

    // --- 1. Definir as Exchanges (apenas para garantir que existem) ---
    @Bean
    public FanoutExchange physicianExchange() { return new FanoutExchange(physicianExchangeName); }

    @Bean
    public FanoutExchange patientExchange() { return new FanoutExchange(patientExchangeName); }

    // --- 2. Filas Anónimas (Uma para cada tópico) ---
    @Bean
    public Queue physicianQueue() { return new AnonymousQueue(); }

    @Bean
    public Queue patientQueue() { return new AnonymousQueue(); }

    // --- 3. Ligações (Bindings) ---
    @Bean
    public Binding bindPhysician(FanoutExchange physicianExchange, @Qualifier("physicianQueue") Queue q) {
        return BindingBuilder.bind(q).to(physicianExchange);
    }

    @Bean
    public Binding bindPatient(FanoutExchange patientExchange, @Qualifier("patientQueue") Queue q) {
        return BindingBuilder.bind(q).to(patientExchange);
    }

    // --- 4. Conversor JSON ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}