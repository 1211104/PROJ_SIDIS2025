package com.example.patientservice.event;

import com.example.patientservice.event.dto.AppointmentEventDTO;
import com.example.patientservice.event.dto.PatientVerifiedEvent;
import com.example.patientservice.repository.PatientRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PatientSagaListener {

    private final RabbitTemplate rabbitTemplate;
    private final PatientRepository repository;

    public PatientSagaListener(RabbitTemplate rabbitTemplate, PatientRepository repository) {
        this.rabbitTemplate = rabbitTemplate;
        this.repository = repository;
    }

    // Ouve a criação de consultas na fila "saga-patient-appointment-listener"
    @RabbitListener(queues = "saga-patient-appointment-listener")
    public void onAppointmentCreated(AppointmentEventDTO event) {
        System.out.println("PATIENT SAGA: Recebi pedido para validar paciente " + event.getPatientNumber());

        // Validação de Negócio
        boolean isVerified = checkPatientStatus(event.getPatientNumber());

        // Criar resposta
        PatientVerifiedEvent response = new PatientVerifiedEvent();
        response.setAppointmentNumber(event.getAppointmentNumber());
        response.setPatientNumber(event.getPatientNumber());
        response.setVerified(isVerified);

        // Enviar para a fila de resposta do AppointmentService
        rabbitTemplate.convertAndSend("patient-response-queue", response);

        System.out.println("PATIENT SAGA: Enviei resposta (Válido: " + isVerified + ")");
    }

    private boolean checkPatientStatus(String patientNumber) {
        // SIMULAÇÃO DE ERRO: Se o ID for "P-FAIL", rejeita
        if ("PT-FAIL".equals(patientNumber)) {
            System.out.println("PATIENT SAGA: Simulação de falha ativada para " + patientNumber);
            return false;
        }

        // Validação Real: O paciente existe na base de dados?
        boolean exists = repository.findByPatientNumber(patientNumber).isPresent();

        if (!exists) {
            System.out.println("PATIENT SAGA: Paciente não encontrado na BD.");
        }

        return exists;
    }
}