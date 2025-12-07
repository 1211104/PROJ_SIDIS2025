package com.example.appointmentservice.event;

import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.model.ConsultationType;
import com.example.appointmentservice.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppointmentSyncConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentSyncConsumer.class);
    private final AppointmentRepository repository;

    public AppointmentSyncConsumer(AppointmentRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "#{appointmentSyncQueue.name}")
    public void receiveAppointmentEvent(AppointmentEvent event) {
        logger.info("--> RabbitMQ [Sync]: Recebi Appointment {} Tipo: {}", event.getAppointmentNumber(), event.getEventType());

        // CREATE e UPDATE (Upsert)
        if ("CREATED".equals(event.getEventType()) || "UPDATED".equals(event.getEventType())) {

            // Tenta encontrar o existente para obter o ID interno
            Optional<Appointment> existing = repository.findByAppointmentNumber(event.getAppointmentNumber());

            Appointment app;
            if (existing.isPresent()) {
                // MODO UPDATE: Usamos o objeto carregado da BD
                app = existing.get();
                logger.info("SINC: Atualizando registo existente {}", event.getAppointmentNumber());
            } else {
                app = new Appointment();
                app.setAppointmentNumber(event.getAppointmentNumber());
                logger.info("SINC: Criando novo registo {}", event.getAppointmentNumber());
            }

            // Atualiza TODOS os campos
            app.setPhysicianNumber(event.getPhysicianNumber());
            app.setPatientNumber(event.getPatientNumber());
            app.setStartTime(event.getStartTime());
            app.setEndTime(event.getEndTime());

            if (event.getStatus() != null)
                app.setStatus(AppointmentStatus.valueOf(event.getStatus()));

            if (event.getConsultationType() != null)
                app.setConsultationType(ConsultationType.valueOf(event.getConsultationType()));

            repository.save(app);
            logger.info("SINC: Dados persistidos com sucesso.");

        } else if ("DELETED".equals(event.getEventType())) {
            repository.findByAppointmentNumber(event.getAppointmentNumber()).ifPresent(app -> {
                repository.delete(app);
                logger.info("SINC: Appointment {} eliminado.", event.getAppointmentNumber());
            });
        }
    }
}