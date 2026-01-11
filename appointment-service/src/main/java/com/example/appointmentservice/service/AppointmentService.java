package com.example.appointmentservice.service;

import com.example.appointmentservice.event.AppointmentProducer;
import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.model.AppointmentEventStore;
import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final ExternalPhysicianRepository physicianRepo;
    private final ExternalPatientRepository patientRepo;
    private final AppointmentEventRepository eventRepository;
    private final AppointmentProducer appointmentProducer;
    private final ObjectMapper objectMapper;

    public AppointmentService(AppointmentRepository appointmentRepo,
                              ExternalPhysicianRepository physicianRepo,
                              ExternalPatientRepository patientRepo,
                              AppointmentEventRepository eventRepository,
                              AppointmentProducer appointmentProducer,
                              ObjectMapper objectMapper) {
        this.appointmentRepo = appointmentRepo;
        this.physicianRepo = physicianRepo;
        this.patientRepo = patientRepo;
        this.eventRepository = eventRepository;
        this.appointmentProducer = appointmentProducer;
        this.objectMapper = objectMapper;
    }

    // LEITURA (Replay de Eventos ou Fallback para BD)

    public Optional<Appointment> findByNumber(String appointmentNumber) {
        // Tenta reconstruir via Event Sourcing (Verdade absoluta)
        Appointment replayed = replayEvents(appointmentNumber);
        if (replayed != null) {
            return Optional.of(replayed);
        }
        // Fallback: Projeção de Leitura (BD Local)
        return appointmentRepo.findByAppointmentNumber(appointmentNumber);
    }

    public List<Appointment> findAll() {
        // Para listas, usamos sempre a projeção de leitura (BD) pois replay de tudo seria lento
        return appointmentRepo.findAll();
    }

    // ESCRITA (Command -> Event Store -> RabbitMQ)

    @Transactional
    public Appointment createAppointment(Appointment body) {
        // 1. Validação CQRS (Dados Locais)
        if (!physicianRepo.existsById(body.getPhysicianNumber())) {
            throw new IllegalArgumentException("Physician inexistente (ou não sincronizado)");
        }
        if (!patientRepo.existsById(body.getPatientNumber())) {
            throw new IllegalArgumentException("Patient inexistente (ou não sincronizado)");
        }


        String newId = UUID.randomUUID().toString();
        body.setAppointmentNumber(newId);
        body.setStatus(AppointmentStatus.PENDING);
        if (body.getStartTime() == null) body.setStartTime(LocalDateTime.now());

        try {
            // Event Sourcing: Preparar e Gravar o evento CREATED
            String jsonPayload = objectMapper.writeValueAsString(body);
            AppointmentEventStore newEvent = new AppointmentEventStore(
                    newId, "APPOINTMENT_CREATED", jsonPayload
            );

            // Guarda o evento na base de dados (Persistence)
            AppointmentEventStore savedEvent = eventRepository.save(newEvent);

            try {
                // RabbitMQ: Avisar o mundo (Saga Start + Sync Réplicas)
                appointmentProducer.sendAppointmentCreated(body);

                System.out.println("SAGA [Success]: Evento gravado e mensagem enviada. ID: " + newId);

            } catch (Exception mqError) {
                // SAGA ROLLBACK (Compensating Transaction)
                // Se o RabbitMQ falhar, desfaz a gravação na base de dados

                System.err.println("SAGA [Failure]: Falha no envio para RabbitMQ. A reverter Event Store...");

                // Apaga o evento explicitamente (Compensação)
                eventRepository.delete(savedEvent);

                // Lança exceção para o cliente (e para garantir rollback da transação Spring se houver)
                throw new RuntimeException("Erro Crítico: Falha na comunicação (Saga). Consulta cancelada.", mqError);
            }

            return body;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao processar JSON", e);
        }
    }

    @Transactional
    public void deleteAppointment(String appointmentNumber) {
        if (appointmentRepo.findByAppointmentNumber(appointmentNumber).isPresent()) {

            appointmentProducer.sendAppointmentDeleted(appointmentNumber);

        }
    }

    // LÓGICA DE REPLAY (Privada)

    private Appointment replayEvents(String appointmentNumber) {
        List<AppointmentEventStore> history = eventRepository.findByAppointmentNumberOrderByOccurredAtAsc(appointmentNumber);
        if (history.isEmpty()) return null;

        Appointment aggregate = new Appointment();
        for (AppointmentEventStore event : history) {
            try {
                switch (event.getEventType()) {
                    case "APPOINTMENT_CREATED":
                        Appointment payload = objectMapper.readValue(event.getPayload(), Appointment.class);
                        // Copiar propriedades
                        aggregate.setAppointmentNumber(payload.getAppointmentNumber());
                        aggregate.setPhysicianNumber(payload.getPhysicianNumber());
                        aggregate.setPatientNumber(payload.getPatientNumber());
                        aggregate.setConsultationType(payload.getConsultationType());
                        aggregate.setStartTime(payload.getStartTime());
                        aggregate.setEndTime(payload.getEndTime());
                        aggregate.setStatus(AppointmentStatus.PENDING);
                        break;
                    case "APPOINTMENT_CANCELLED":
                        aggregate.setStatus(AppointmentStatus.CANCELLED);
                        break;
                    case "APPOINTMENT_CONFIRMED":
                        aggregate.setStatus(AppointmentStatus.CONFIRMED);
                        break;
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return aggregate;
    }
}