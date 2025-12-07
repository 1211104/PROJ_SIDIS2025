package com.example.appointmentservice.event;

import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.model.ConsultationType;
import com.example.appointmentservice.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class AppointmentSyncConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentSyncConsumer.class);
    private final AppointmentRepository repository;

    public AppointmentSyncConsumer(AppointmentRepository repository) {
        this.repository = repository;
    }

    // Ouve a fila de sync de appointments da réplica
    @RabbitListener(queues = "#{appointmentSyncQueue.name}")
    public void receiveAppointmentEvent(AppointmentEvent event) {
        logger.info("--> RabbitMQ: Recebi Appointment {} do tipo {}", event.getAppointmentNumber(), event.getEventType());

        if ("CREATED".equals(event.getEventType())) {
            if (repository.findByAppointmentNumber(event.getAppointmentNumber()).isEmpty()) {
                Appointment app = new Appointment();
                app.setAppointmentNumber(event.getAppointmentNumber());
                app.setPhysicianNumber(event.getPhysicianNumber());
                app.setPatientNumber(event.getPatientNumber());
                app.setStartTime(event.getStartTime());
                app.setEndTime(event.getEndTime());

                app.setStatus(AppointmentStatus.valueOf(event.getStatus()));
                app.setConsultationType(ConsultationType.valueOf(event.getConsultationType()));

                repository.save(app);
                logger.info("SINC: Appointment {} guardado localmente.", event.getAppointmentNumber());
            }
        }
        else if ("DELETED".equals(event.getEventType())) {
            repository.findByAppointmentNumber(event.getAppointmentNumber()).ifPresentOrElse(
                    app -> {
                        repository.delete(app);
                        logger.info("SINC: Appointment {} eliminado localmente por evento externo.", event.getAppointmentNumber());
                    },
                    () -> logger.warn("SINC: Tentativa de apagar Appointment {} que não existe localmente.", event.getAppointmentNumber())
            );
        }
    }
}