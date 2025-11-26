package com.example.physiciansservice.event;

import com.example.physiciansservice.model.Physician;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PhysicianProducer {

    private static final Logger logger = LoggerFactory.getLogger(PhysicianProducer.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${hap.rabbitmq.exchange.physicians}")
    private String exchangeName;

    public PhysicianProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPhysicianCreated(Physician physician) {
        // Converte a Entidade da BD num Evento DTO
        PhysicianEvent event = new PhysicianEvent(
                physician.getPhysicianNumber(),
                physician.getName(),
                physician.getSpecialty(),
                "CREATED"
        );

        logger.info("--> RabbitMQ: A enviar evento CREATED para o médico {}", physician.getPhysicianNumber());

        // Envia para a Exchange, sem routing key ("") pois é Fanout
        rabbitTemplate.convertAndSend(exchangeName, "", event);
    }
}
