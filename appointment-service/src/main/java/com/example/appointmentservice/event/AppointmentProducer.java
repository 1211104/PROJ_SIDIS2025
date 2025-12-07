package com.example.appointmentservice.event;

import com.example.appointmentservice.model.Appointment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppointmentProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${hap.rabbitmq.exchange.appointments:appointment-exchange}")
    private String exchangeName;

    public AppointmentProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendAppointmentCreated(Appointment app) {
        AppointmentEvent event = new AppointmentEvent(
                app.getAppointmentNumber(),
                app.getPhysicianNumber(),
                app.getPatientNumber(),
                app.getStartTime(),
                app.getEndTime(),
                app.getStatus().name(),
                app.getConsultationType().name(),
                "CREATED"
        );
        rabbitTemplate.convertAndSend(exchangeName, "", event);
    }

    public void sendAppointmentDeleted(String appointmentNumber) {
        AppointmentEvent event = new AppointmentEvent();
        event.setAppointmentNumber(appointmentNumber);
        event.setEventType("DELETED");

        rabbitTemplate.convertAndSend(exchangeName, "", event);
    }
}