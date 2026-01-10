package com.example.patientservice.event;

import com.example.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PatientProducer {

    private static final Logger logger = LoggerFactory.getLogger(PatientProducer.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${hap.rabbitmq.exchange.patients}")
    private String exchangeName;

    public PatientProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPatientCreated(Patient p) {
        sendEvent(p, "CREATED");
    }

    public void sendPatientUpdated(Patient p) {
        sendEvent(p, "UPDATED");
    }

    public void sendPatientDeleted(String patientNumber) {
        // Para delete basta o ID e o tipo
        PatientEvent event = new PatientEvent(patientNumber, null, null, "DELETED");
        logger.info("--> RabbitMQ: A enviar evento DELETED para {}", patientNumber);
        rabbitTemplate.convertAndSend(exchangeName, "", event);
    }

    private void sendEvent(Patient p, String type) {
        PatientEvent event = new PatientEvent(
                p.getPatientNumber(),
                p.getName(),
                p.getPhoneNumber(),
                type
        );
        logger.info("--> RabbitMQ: A enviar evento {} para {}", type, p.getPatientNumber());
        rabbitTemplate.convertAndSend(exchangeName, "", event);
    }
}