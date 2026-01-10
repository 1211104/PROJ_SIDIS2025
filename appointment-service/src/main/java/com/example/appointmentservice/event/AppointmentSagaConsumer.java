package com.example.appointmentservice.event;

import com.example.appointmentservice.event.dto.PatientVerifiedEvent;
import com.example.appointmentservice.event.dto.PhysicianReservedEvent;
import com.example.appointmentservice.model.AppointmentEventStore;
import com.example.appointmentservice.repository.AppointmentEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentSagaConsumer {

    private final AppointmentEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public AppointmentSagaConsumer(AppointmentEventRepository eventRepository) {
        this.eventRepository = eventRepository;
        this.objectMapper = new ObjectMapper();
    }

    // =========================================================
    // OUVIR RESPOSTA DO MÉDICO
    // =========================================================
    @RabbitListener(queues = "physician-response-queue")
    @Transactional
    public void handlePhysicianResponse(PhysicianReservedEvent event) {
        System.out.println("SAGA: Recebi resposta do Médico para " + event.getAppointmentNumber());

        if (event.isAvailable()) {
            // Grava sucesso do médico
            appendEvent(event.getAppointmentNumber(), "PHYSICIAN_RESERVED", event);
            // Verifica se já podemos fechar a Saga com sucesso
            checkAndConfirmSaga(event.getAppointmentNumber());
        } else {
            // Médico rejeitou -> Falha a Saga imediatamente
            appendEvent(event.getAppointmentNumber(), "APPOINTMENT_CANCELLED", "Physician Unavailable");
        }
    }

    // =========================================================
    // 2. OUVIR RESPOSTA DO PACIENTE
    // =========================================================
    @RabbitListener(queues = "patient-response-queue")
    @Transactional
    public void handlePatientResponse(PatientVerifiedEvent event) {
        System.out.println("SAGA: Recebi resposta do Paciente para " + event.getAppointmentNumber());

        if (event.isVerified()) {
            // Grava sucesso do paciente
            appendEvent(event.getAppointmentNumber(), "PATIENT_VERIFIED", event);
            // Verifica se já podemos fechar a Saga
            checkAndConfirmSaga(event.getAppointmentNumber());
        } else {
            // Paciente inválido -> Falha a Saga
            appendEvent(event.getAppointmentNumber(), "APPOINTMENT_CANCELLED", "Patient Invalid");
        }
    }

    // =========================================================
    // LÓGICA DE DECISÃO (EVENT SOURCING)
    // =========================================================
    private void checkAndConfirmSaga(String appointmentNumber) {
        // Buscar histórico atual
        List<AppointmentEventStore> history = eventRepository
                .findByAppointmentNumberOrderByOccurredAtAsc(appointmentNumber);

        boolean physicianOk = false;
        boolean patientOk = false;
        boolean alreadyFinalized = false;

        // Analisar histórico
        for (AppointmentEventStore evt : history) {
            if ("PHYSICIAN_RESERVED".equals(evt.getEventType())) physicianOk = true;
            if ("PATIENT_VERIFIED".equals(evt.getEventType())) patientOk = true;
            if ("APPOINTMENT_CONFIRMED".equals(evt.getEventType())) alreadyFinalized = true;
            if ("APPOINTMENT_CANCELLED".equals(evt.getEventType())) alreadyFinalized = true;
        }

        // Decisão: Se ambos OK e ainda não finalizado -> CONFIRMA
        if (physicianOk && patientOk && !alreadyFinalized) {
            System.out.println("SAGA: Sucesso Total! Confirmando consulta " + appointmentNumber);
            appendEvent(appointmentNumber, "APPOINTMENT_CONFIRMED", "All checks passed");
        }
    }

    // Método auxiliar para gravar evento
    private void appendEvent(String appNum, String type, Object payload) {
        try {
            String json = (payload instanceof String) ? (String) payload : objectMapper.writeValueAsString(payload);
            eventRepository.save(new AppointmentEventStore(appNum, type, json));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}