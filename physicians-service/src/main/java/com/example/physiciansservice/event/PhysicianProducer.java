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
        sendEvent(physician, "CREATED");
    }

    public void sendPhysicianUpdated(Physician physician) {
        sendEvent(physician, "UPDATED");
    }

    public void sendPhysicianDeleted(String physicianNumber) {
        // Para apagar, necessario ID e do tipo de evento
        PhysicianEvent event = new PhysicianEvent(physicianNumber, null, null, null, "DELETED");
        logger.info("--> RabbitMQ: A enviar evento DELETED para {}", physicianNumber);
        rabbitTemplate.convertAndSend(exchangeName, "", event);
    }

    private void sendEvent(Physician p, String type) {
        PhysicianEvent event = new PhysicianEvent(
                p.getPhysicianNumber(),
                p.getName(),
                p.getSpecialty(),
                p.getContactInfo(),
                type
        );
        logger.info("--> RabbitMQ: A enviar evento {} para {}", type, p.getPhysicianNumber());
        rabbitTemplate.convertAndSend(exchangeName, "", event);
    }
}