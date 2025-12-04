package com.example.appointmentservice.event;

import com.example.appointmentservice.model.ExternalPatient;
import com.example.appointmentservice.model.ExternalPhysician;
import com.example.appointmentservice.repository.ExternalPatientRepository;
import com.example.appointmentservice.repository.ExternalPhysicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class DataSyncConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DataSyncConsumer.class);
    private final ExternalPhysicianRepository physicianRepo;
    private final ExternalPatientRepository patientRepo;

    public DataSyncConsumer(ExternalPhysicianRepository physicianRepo, ExternalPatientRepository patientRepo) {
        this.physicianRepo = physicianRepo;
        this.patientRepo = patientRepo;
    }

    // OUVIR Physician
    @RabbitListener(queues = "#{physicianQueue.name}")
    public void handlePhysicianEvent(PhysicianEvent event) {
        logger.info("Recebido Evento Médico: {} - {}", event.getEventType(), event.getPhysicianNumber());

        if ("DELETED".equals(event.getEventType())) {
            physicianRepo.deleteById(event.getPhysicianNumber());
        } else {
            // CREATED ou UPDATED -> Upsert
            ExternalPhysician p = new ExternalPhysician(event.getPhysicianNumber(), event.getName());
            physicianRepo.save(p);
        }
    }

    // OUVIR PACIENTS
    @RabbitListener(queues = "#{patientQueue.name}")
    public void handlePatientEvent(PatientEvent event) {
        logger.info("Recebido Evento Paciente: {} - {}", event.getEventType(), event.getPatientNumber());

        if ("DELETED".equals(event.getEventType())) {
            patientRepo.deleteById(event.getPatientNumber());
        } else {
            // CREATED ou UPDATED -> Upsert
            ExternalPatient p = new ExternalPatient(event.getPatientNumber(), event.getName());
            patientRepo.save(p);
        }
    }
}