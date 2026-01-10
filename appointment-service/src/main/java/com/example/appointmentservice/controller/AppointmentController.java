package com.example.appointmentservice.controller;

import com.example.appointmentservice.event.AppointmentProducer;
import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.model.AppointmentEventStore;
import com.example.appointmentservice.repository.AppointmentEventRepository;
import com.example.appointmentservice.repository.AppointmentRepository;
import com.example.appointmentservice.repository.ExternalPatientRepository;
import com.example.appointmentservice.repository.ExternalPhysicianRepository;

// Imports para JSON e UUID
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    // REPOSITÓRIOS LEGACY (Para Search, Delete, Patch)
    private final AppointmentRepository appointmentRepo;

    // REPOSITÓRIOS CQRS (Validação de Leitura)
    private final ExternalPhysicianRepository physicianRepo;
    private final ExternalPatientRepository patientRepo;

    // REPOSITÓRIO DE EVENTOS (Event Sourcing)
    private final AppointmentEventRepository eventRepository;

    private final AppointmentProducer appointmentProducer;

    // Para converter Objetos em JSON
    private final ObjectMapper objectMapper;

    // CONSTRUTOR COMPLETO
    public AppointmentController(AppointmentRepository appointmentRepo,
                                 ExternalPhysicianRepository physicianRepo,
                                 ExternalPatientRepository patientRepo,
                                 AppointmentEventRepository eventRepository,
                                 AppointmentProducer appointmentProducer) {
        this.appointmentRepo = appointmentRepo;
        this.physicianRepo = physicianRepo;
        this.patientRepo = patientRepo;
        this.eventRepository = eventRepository;
        this.appointmentProducer = appointmentProducer;

        // Configura o Jackson para aceitar datas (LocalDateTime)
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }


    //  MÉTODO AUXILIAR: REPLAY (Reconstrói o estado atual)
    private Appointment replayEvents(String appointmentNumber) {
        // Buscar histórico ordenado por data
        List<AppointmentEventStore> history = eventRepository.findByAppointmentNumberOrderByOccurredAtAsc(appointmentNumber);

        if (history.isEmpty()) return null;

        Appointment aggregate = new Appointment();

        // Aplicar eventos sequencialmente
        for (AppointmentEventStore event : history) {
            try {
                switch (event.getEventType()) {
                    case "APPOINTMENT_CREATED":
                        // Reconstrói a partir do JSON guardado
                        Appointment payloadApp = objectMapper.readValue(event.getPayload(), Appointment.class);

                        aggregate.setAppointmentNumber(payloadApp.getAppointmentNumber());
                        aggregate.setPhysicianNumber(payloadApp.getPhysicianNumber());
                        aggregate.setPatientNumber(payloadApp.getPatientNumber());
                        aggregate.setConsultationType(payloadApp.getConsultationType());
                        aggregate.setStartTime(payloadApp.getStartTime());
                        aggregate.setEndTime(payloadApp.getEndTime());
                        aggregate.setStatus(AppointmentStatus.PENDING); // Estado inicial
                        break;

                    case "APPOINTMENT_CANCELLED":
                        aggregate.setStatus(AppointmentStatus.CANCELLED);
                        break;

                    case "APPOINTMENT_CONFIRMED":
                        aggregate.setStatus(AppointmentStatus.CONFIRMED);
                        break;

                    case "APPOINTMENT_REJECTED": // Caso falhe
                        aggregate.setStatus(AppointmentStatus.REJECTED);
                        break;
                }

            } catch (JsonProcessingException e) {
                System.err.println("Erro ao processar JSON do evento ID: " + event.getId());
                e.printStackTrace();
            }
        }
        return aggregate;
    }

    // ==========================================
    //  LEITURAS (USANDO EVENT SOURCING)
    // ==========================================

    @GetMapping("/by-number/{appointmentNumber}")
    public ResponseEntity<Appointment> getByNumber(@PathVariable String appointmentNumber) {
        Appointment app = replayEvents(appointmentNumber);

        if (app == null) {
            // Fallback: Se não encontrar nos eventos, tenta na BD antiga (durante a migração)
            return appointmentRepo.findByAppointmentNumber(appointmentNumber)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        return ResponseEntity.ok(app);
    }

    // ==========================================
    //  ESCRITA (CRIAÇÃO VIA EVENT SOURCING)
    // ==========================================

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Appointment body) {
        // Validações CQRS
        if (body.getPhysicianNumber() == null || body.getPhysicianNumber().isBlank())
            return ResponseEntity.badRequest().body("physicianNumber é obrigatório");

        // Verifica se existe na Projeção Local (tabela externa)
        if (!physicianRepo.existsById(body.getPhysicianNumber())) {
            return ResponseEntity.status(404).body("Physician inexistente (ou não sincronizado)");
        }
        if (body.getPatientNumber() == null || !patientRepo.existsById(body.getPatientNumber())) {
            return ResponseEntity.status(404).body("Patient inexistente (ou não sincronizado)");
        }

        // Gerar ID e Estado Inicial
        String newId = UUID.randomUUID().toString();
        body.setAppointmentNumber(newId);
        body.setStatus(AppointmentStatus.PENDING); // Nasce como PENDING
        if (body.getStartTime() == null) body.setStartTime(LocalDateTime.now());

        try {
            // Serializar para JSON
            String jsonPayload = objectMapper.writeValueAsString(body);

            // Criar o Evento
            AppointmentEventStore newEvent = new AppointmentEventStore(
                    newId,
                    "APPOINTMENT_CREATED",
                    jsonPayload
            );

            // Gravar na Tabela de Eventos (Event Store)
            eventRepository.save(newEvent);

            // Enviar para RabbitMQ (Início da Saga)
            appointmentProducer.sendAppointmentCreated(body);

            // Retorna 202 Accepted
            return ResponseEntity.accepted()
                    .location(URI.create("/api/appointments/by-number/" + newId))
                    .body(body);

        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().body("Erro ao processar JSON");
        }
    }

    // ==================================================================
    //  MÉTODOS LEGACY
    // ==================================================================

    @GetMapping
    public List<Appointment> search(@RequestParam(required = false) String physician,
                                    @RequestParam(required = false) String patient) {
        if (physician != null && !physician.isBlank())
            return appointmentRepo.findByPhysicianNumber(physician, Pageable.unpaged()).getContent();
        if (patient != null && !patient.isBlank())
            return appointmentRepo.findByPatientNumber(patient, Pageable.unpaged()).getContent();

        return appointmentRepo.findAll();
    }

    @DeleteMapping("/by-number/{appointmentNumber}")
    @Transactional
    public ResponseEntity<?> deleteByNumber(@PathVariable String appointmentNumber) {
        Optional<Appointment> found = appointmentRepo.findByAppointmentNumber(appointmentNumber);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        appointmentRepo.delete(found.get());
        appointmentProducer.sendAppointmentDeleted(appointmentNumber);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/by-number/{appointmentNumber}")
    @Transactional
    public ResponseEntity<?> patchByNumber(@PathVariable String appointmentNumber,
                                           @RequestBody com.example.appointmentservice.repository.AppointmentPatchRequest patch) {
        Optional<Appointment> opt = appointmentRepo.findByAppointmentNumber(appointmentNumber);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Appointment appointment = opt.get();

        if (patch.getStatus() != null) appointment.setStatus(patch.getStatus());

        appointmentRepo.save(appointment);
        appointmentProducer.sendAppointmentUpdated(appointment);
        return ResponseEntity.ok(appointment);
    }
}