package com.example.appointmentservice.event;

import com.example.appointmentservice.event.dto.PatientVerifiedEvent;
import com.example.appointmentservice.event.dto.PhysicianReservedEvent;
import com.example.appointmentservice.model.AppointmentEventStore;
import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.repository.AppointmentEventRepository;
import com.example.appointmentservice.repository.AppointmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentSagaConsumer {

    private final AppointmentEventRepository eventRepository;
    private final AppointmentRepository appointmentRepository; // Repositório para atualizar a tabela principal
    private final ObjectMapper objectMapper;

    public AppointmentSagaConsumer(AppointmentEventRepository eventRepository,
                                   AppointmentRepository appointmentRepository) {
        this.eventRepository = eventRepository;
        this.appointmentRepository = appointmentRepository;
        this.objectMapper = new ObjectMapper();
    }

    // =========================================================
    // OUVIR RESPOSTA DO MÉDICO
    // =========================================================
    @RabbitListener(queues = "physician-response-queue")
    public void handlePhysicianResponse(PhysicianReservedEvent event) {
        System.out.println("SAGA: Recebi resposta do Médico para " + event.getAppointmentNumber());

        if (event.isAvailable()) {
            // Grava o evento de sucesso
            appendEvent(event.getAppointmentNumber(), "PHYSICIAN_RESERVED", event);
            // Verifica se já podemos confirmar a Saga
            checkAndConfirmSaga(event.getAppointmentNumber());
        } else {
            // Médico indisponível -> Cancela tudo
            cancelAppointment(event.getAppointmentNumber(), "Physician Unavailable");
        }
    }

    // =========================================================
    // OUVIR RESPOSTA DO PACIENTE
    // =========================================================
    @RabbitListener(queues = "patient-response-queue")
    public void handlePatientResponse(PatientVerifiedEvent event) {
        System.out.println("SAGA: Recebi resposta do Paciente para " + event.getAppointmentNumber());

        if (event.isVerified()) {
            // Grava o evento de sucesso
            appendEvent(event.getAppointmentNumber(), "PATIENT_VERIFIED", event);
            // Verifica se já podemos confirmar a Saga
            checkAndConfirmSaga(event.getAppointmentNumber());
        } else {
            // Paciente inválido -> Cancela tudo
            cancelAppointment(event.getAppointmentNumber(), "Patient Invalid");
        }
    }

    // =========================================================
    // LÓGICA DE DECISÃO (CORE DA SAGA)
    // =========================================================
    private void checkAndConfirmSaga(String appointmentNumber) {
        // Pausa estratégica para evitar Race Conditions (dá tempo à outra thread de fazer commit)
        try { Thread.sleep(500); } catch (InterruptedException e) { }

        // Ler histórico de eventos
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

        // Se ambos aceitaram e ainda não finalizámos -> CONFIRMAR
        if (physicianOk && patientOk && !alreadyFinalized) {
            System.out.println("SAGA: Sucesso Total! Confirmando consulta " + appointmentNumber);

            // Gravar evento de confirmação (Write Model)
            appendEvent(appointmentNumber, "APPOINTMENT_CONFIRMED", "All checks passed");

            // Atualizar a entidade na BD (Read Model) para o GET funcionar
            updateReadModelStatus(appointmentNumber, "CONFIRMED");
        }
    }

    private void cancelAppointment(String appointmentNumber, String reason) {
        System.out.println("SAGA: A cancelar consulta " + appointmentNumber + " Motivo: " + reason);

        // Gravar evento de cancelamento
        appendEvent(appointmentNumber, "APPOINTMENT_CANCELLED", reason);

        // Atualizar a entidade na BD
        updateReadModelStatus(appointmentNumber, "CANCELLED");
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================

    /**
     * Atualiza o estado na tabela principal 'Appointment' para que o endpoint GET
     * mostre o estado correto ao utilizador.
     */
    private void updateReadModelStatus(String appointmentNumber, String newStatus) {
        appointmentRepository.findByAppointmentNumber(appointmentNumber).ifPresent(app -> {

            try {
                app.setStatus(AppointmentStatus.valueOf(newStatus));
                appointmentRepository.save(app);
                System.out.println("READ MODEL: Estado atualizado para " + newStatus);
            } catch (IllegalArgumentException e) {
                System.err.println("ERRO: O estado '" + newStatus + "' não existe no Enum AppointmentStatus!");
            }
        });
    }

    /**
     * Grava um evento no Event Store.
     * Usa saveAndFlush para garantir visibilidade imediata (evitar stale reads).
     */
    private void appendEvent(String appNum, String type, Object payload) {
        try {
            String json = (payload instanceof String) ? (String) payload : objectMapper.writeValueAsString(payload);
            AppointmentEventStore evt = new AppointmentEventStore(appNum, type, json);

            // IMPORTANTE: saveAndFlush força o envio para a BD imediatamente
            eventRepository.saveAndFlush(evt);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}