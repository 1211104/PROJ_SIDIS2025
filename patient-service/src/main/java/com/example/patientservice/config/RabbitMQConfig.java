package com.example.patientservice.config;

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

    // CONFIGURAÇÃO SYNC (CQRS)
    @Value("${hap.rabbitmq.exchange.patients}")
    private String exchangeName;

    @Value("${INSTANCE_ID}")
    private String instanceId;

    @Bean
    public FanoutExchange patientExchange() {
        return new FanoutExchange(exchangeName);
    }

    @Bean
    public Queue syncQueue() {
        String queueName = "patient-sync-queue-" + instanceId;
        return new Queue(queueName, true, false, false);
    }

    @Bean
    public Binding binding(@Qualifier("patientExchange") FanoutExchange exchange,
                           @Qualifier("syncQueue") Queue syncQueue) {
        return BindingBuilder.bind(syncQueue).to(exchange);
    }

    // ==========================================
    // CONFIGURAÇÃO PARA A SAGA
    // ==========================================

    @Value("${hap.rabbitmq.exchange.appointments:appointment-exchange}")
    private String appointmentExchangeName;

    // Exchange dos Appointments (necessário para ouvir eventos)
    @Bean
    public FanoutExchange appointmentExchange() {
        return new FanoutExchange(appointmentExchangeName);
    }

    // Fila da Saga (Worker Queue - SEM ID de instância)
    // Para validar consultas (Load Balanced entre réplicas)
    @Bean
    public Queue sagaPatientQueue() {
        return new Queue("saga-patient-appointment-listener", true);
    }

    // Ligar a Fila à Exchange dos Appointments
    @Bean
    public Binding bindSagaQueue(@Qualifier("appointmentExchange") FanoutExchange appointmentExchange,
                                 @Qualifier("sagaPatientQueue") Queue q) {
        return BindingBuilder.bind(q).to(appointmentExchange);
    }

    // Fila de RESPOSTA (Para onde vamos enviar o "Válido/Inválido")
    // O AppointmentService está à escuta exatamente neste nome
    @Bean
    public Queue responseQueue() {
        return new Queue("patient-response-queue", true);
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