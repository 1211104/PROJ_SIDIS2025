package com.example.patientservice.event;

import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PatientConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PatientConsumer.class);
    private final PatientRepository repository;

    public PatientConsumer(PatientRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "#{syncQueue.name}")
    public void receivePatientEvent(PatientEvent event) {
        logger.info("--> RabbitMQ: Recebi evento {} para {}", event.getEventType(), event.getPatientNumber());

        switch (event.getEventType()) {
            case "CREATED":
                if (repository.findByPatientNumber(event.getPatientNumber()).isEmpty()) {
                    savePatientFromEvent(event);
                    logger.info("SINC: Paciente {} criado.", event.getPatientNumber());
                } else {
                    logger.info("SINC: Paciente {} já existe.", event.getPatientNumber());
                }
                break;

            case "UPDATED": // Lógica de Upsert
                repository.findByPatientNumber(event.getPatientNumber()).ifPresentOrElse(
                        p -> {
                            updatePatientFromEvent(p, event);
                            repository.save(p);
                            logger.info("SINC: Paciente {} atualizado.", event.getPatientNumber());
                        },
                        () -> {
                            savePatientFromEvent(event);
                            logger.info("SINC: Upsert - Paciente {} criado via update.", event.getPatientNumber());
                        }
                );
                break;

            case "DELETED":
                repository.findByPatientNumber(event.getPatientNumber()).ifPresent(p -> {
                    repository.delete(p);
                    logger.info("✅ SINC: Paciente {} apagado.", event.getPatientNumber());
                });
                break;
        }
    }

    private void savePatientFromEvent(PatientEvent event) {
        Patient p = new Patient();
        p.setPatientNumber(event.getPatientNumber());
        p.setName(event.getName());
        p.setPhoneNumber(event.getPhoneNumber());
        repository.save(p);
    }

    private void updatePatientFromEvent(Patient p, PatientEvent event) {
        p.setName(event.getName());
        p.setPhoneNumber(event.getPhoneNumber());
    }
}