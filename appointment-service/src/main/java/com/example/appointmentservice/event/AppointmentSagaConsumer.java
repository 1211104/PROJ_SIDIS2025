package com.example.appointmentservice.event;

import com.example.appointmentservice.event.dto.PatientVerifiedEvent;
import com.example.appointmentservice.event.dto.PhysicianReservedEvent;
import com.example.appointmentservice.model.AppointmentEventStore;
import com.example.appointmentservice.repository.AppointmentEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

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
    // Garante que o appendEvent faz commit antes de entrar no checkAndConfirmSaga
    @RabbitListener(queues = "physician-response-queue")
    public void handlePhysicianResponse(PhysicianReservedEvent event) {
        System.out.println("SAGA: Recebi resposta do Médico para " + event.getAppointmentNumber());

        if (event.isAvailable()) {
            appendEvent(event.getAppointmentNumber(), "PHYSICIAN_RESERVED", event);
            // Agora que já gravou e COMITOU, podemos verificar (e dormir se preciso)
            checkAndConfirmSaga(event.getAppointmentNumber());
        } else {
            appendEvent(event.getAppointmentNumber(), "APPOINTMENT_CANCELLED", "Physician Unavailable");
        }
    }

    // =========================================================
    // OUVIR RESPOSTA DO PACIENTE
    // =========================================================
    @RabbitListener(queues = "patient-response-queue")
    public void handlePatientResponse(PatientVerifiedEvent event) {
        System.out.println("SAGA: Recebi resposta do Paciente para " + event.getAppointmentNumber());

        if (event.isVerified()) {
            appendEvent(event.getAppointmentNumber(), "PATIENT_VERIFIED", event);
            checkAndConfirmSaga(event.getAppointmentNumber());
        } else {
            appendEvent(event.getAppointmentNumber(), "APPOINTMENT_CANCELLED", "Patient Invalid");
        }
    }

    // =========================================================
    // LÓGICA DE DECISÃO (EVENT SOURCING)
    // =========================================================
    private void checkAndConfirmSaga(String appointmentNumber) {
        // Pausa para garantir que a OUTRA thread (que pode estar a correr em paralelo) tenha tempo de fazer o seu Commit.
        try { Thread.sleep(500); } catch (InterruptedException e) { }

        List<AppointmentEventStore> history = eventRepository
                .findByAppointmentNumberOrderByOccurredAtAsc(appointmentNumber);

        boolean physicianOk = false;
        boolean patientOk = false;
        boolean alreadyFinalized = false;

        for (AppointmentEventStore evt : history) {
            if ("PHYSICIAN_RESERVED".equals(evt.getEventType())) physicianOk = true;
            if ("PATIENT_VERIFIED".equals(evt.getEventType())) patientOk = true;
            if ("APPOINTMENT_CONFIRMED".equals(evt.getEventType())) alreadyFinalized = true;
            if ("APPOINTMENT_CANCELLED".equals(evt.getEventType())) alreadyFinalized = true;
        }

        // Se ambos OK e ainda não finalizado -> CONFIRMA!
        if (physicianOk && patientOk && !alreadyFinalized) {
            System.out.println("SAGA: Sucesso Total! Confirmando consulta " + appointmentNumber);
            appendEvent(appointmentNumber, "APPOINTMENT_CONFIRMED", "All checks passed");
        }
    }

    // Método auxiliar para gravar evento
    private void appendEvent(String appNum, String type, Object payload) {
        try {
            String json = (payload instanceof String) ? (String) payload : objectMapper.writeValueAsString(payload);
            AppointmentEventStore evt = new AppointmentEventStore(appNum, type, json);

            eventRepository.saveAndFlush(evt);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}