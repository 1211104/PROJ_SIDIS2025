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

    // Exchanges para CQRS (Broadcast)
    @Value("${hap.rabbitmq.exchange.physicians}")
    private String physicianExchangeName;

    @Value("${hap.rabbitmq.exchange.patients}")
    private String patientExchangeName;

    @Value("${hap.rabbitmq.exchange.appointments:appointment-exchange}")
    private String appointmentExchangeName;

    // INJETAR O ID DA INSTANCIA (Crucial para CQRS com DB local)
    @Value("${INSTANCE_ID:${random.value}}")
    private String instanceId;

    // =======================================
    // BEANS PARA SINCRONIZAÇÃO (CQRS)
    // =======================================

    @Bean
    public FanoutExchange physicianExchange() { return new FanoutExchange(physicianExchangeName); }

    @Bean
    public FanoutExchange patientExchange() { return new FanoutExchange(patientExchangeName); }

    @Bean
    public FanoutExchange appointmentExchange() { return new FanoutExchange(appointmentExchangeName); }

    // Filas Únicas por Instância (Broadcast)
    @Bean
    public Queue physicianQueue() {
        return new Queue("appointment-physician-sync-queue-" + instanceId, true);
    }

    @Bean
    public Queue patientQueue() {
        return new Queue("appointment-patient-sync-queue-" + instanceId, true);
    }

    @Bean
    public Queue appointmentSyncQueue() {
        return new Queue("appointment-sync-queue-" + instanceId, true);
    }

    // Bindings CQRS (COM QUALIFIERS PARA SEGURANÇA)
    @Bean
    public Binding bindPhysician(@Qualifier("physicianExchange") FanoutExchange physicianExchange,
                                 @Qualifier("physicianQueue") Queue q) {
        return BindingBuilder.bind(q).to(physicianExchange);
    }

    @Bean
    public Binding bindPatient(@Qualifier("patientExchange") FanoutExchange patientExchange,
                               @Qualifier("patientQueue") Queue q) {
        return BindingBuilder.bind(q).to(patientExchange);
    }

    @Bean
    public Binding bindAppointment(@Qualifier("appointmentExchange") FanoutExchange appointmentExchange,
                                   @Qualifier("appointmentSyncQueue") Queue q) {
        return BindingBuilder.bind(q).to(appointmentExchange);
    }

    // =======================================
    // BEANS PARA A SAGA (NOVO)
    // =======================================

    // Estas filas NÃO têm instanceId. São partilhadas entre réplicas (Worker Pattern).

    @Bean
    public Queue physicianResponseQueue() {
        return new Queue("physician-response-queue", true);
    }

    @Bean
    public Queue patientResponseQueue() {
        return new Queue("patient-response-queue", true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}