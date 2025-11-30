package com.example.appointmentservice.controller;

import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.model.ConsultationType;
import com.example.appointmentservice.repository.AppointmentRepository;
import com.example.appointmentservice.repository.ExternalPatientRepository;
import com.example.appointmentservice.repository.ExternalPhysicianRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository appointmentRepo;

    // Repositórios CQRS (Tabelas de Leitura locais)
    private final ExternalPhysicianRepository physicianRepo;
    private final ExternalPatientRepository patientRepo;

    public AppointmentController(AppointmentRepository appointmentRepo,
                                 ExternalPhysicianRepository physicianRepo,
                                 ExternalPatientRepository patientRepo) {
        this.appointmentRepo = appointmentRepo;
        this.physicianRepo = physicianRepo;
        this.patientRepo = patientRepo;
    }

    // ==========================================
    // LEITURAS (Apenas na BD Local)
    // ==========================================

    @GetMapping
    public List<Appointment> search(@RequestParam(required = false) String physician,
                                    @RequestParam(required = false) String patient) {
        // Pesquisa simples na BD local
        if (physician != null && !physician.isBlank())
            return appointmentRepo.findByPhysicianNumber(physician, Pageable.unpaged()).getContent();
        if (patient != null && !patient.isBlank())
            return appointmentRepo.findByPatientNumber(patient, Pageable.unpaged()).getContent();

        return appointmentRepo.findAll();
    }

    @GetMapping("/by-number/{appointmentNumber}")
    public ResponseEntity<Appointment> getByNumber(@PathVariable String appointmentNumber) {
        return appointmentRepo.findByAppointmentNumber(appointmentNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // ESCRITAS (Validação Local via CQRS)
    // ==========================================

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Appointment body) {
        // 1. Validações Básicas
        if (body.getAppointmentNumber() == null || body.getAppointmentNumber().isBlank())
            return ResponseEntity.badRequest().body("appointmentNumber é obrigatório");
        if (body.getPhysicianNumber() == null || body.getPhysicianNumber().isBlank())
            return ResponseEntity.badRequest().body("physicianNumber é obrigatório");
        if (body.getPatientNumber() == null || body.getPatientNumber().isBlank())
            return ResponseEntity.badRequest().body("patientNumber é obrigatório");

        // 2. Unicidade (Local)
        if (appointmentRepo.findByAppointmentNumber(body.getAppointmentNumber()).isPresent())
            return ResponseEntity.status(409).body("appointmentNumber já existe");

        // 3. VALIDAÇÃO CQRS (A grande mudança!)
        // Verifica nas tabelas locais se os dados existem (sincronizados via RabbitMQ)

        if (!physicianRepo.existsById(body.getPhysicianNumber())) {
            return ResponseEntity.status(404).body("physicianNumber inexistente (ou ainda não sincronizado)");
        }

        if (!patientRepo.existsById(body.getPatientNumber())) {
            return ResponseEntity.status(404).body("patientNumber inexistente (ou ainda não sincronizado)");
        }

        // 4. Valores Default e Gravação
        if (body.getStatus() == null) body.setStatus(AppointmentStatus.SCHEDULED);
        if (body.getConsultationType() == null) body.setConsultationType(ConsultationType.IN_PERSON);
        if (body.getStartTime() == null) body.setStartTime(LocalDateTime.now());

        Appointment saved = appointmentRepo.save(body);

        return ResponseEntity.created(URI.create("/api/appointments/by-number/" + saved.getAppointmentNumber()))
                .body(saved);
    }

    @DeleteMapping("/by-number/{appointmentNumber}")
    @Transactional
    public ResponseEntity<?> deleteByNumber(@PathVariable String appointmentNumber) {
        Optional<Appointment> found = appointmentRepo.findByAppointmentNumber(appointmentNumber);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        appointmentRepo.delete(found.get());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/by-number/{appointmentNumber}")
    @Transactional
    public ResponseEntity<?> patchByNumber(@PathVariable String appointmentNumber,
                                           @RequestBody com.example.appointmentservice.repository.AppointmentPatchRequest patch) {

        // 1. Verificar se a marcação existe
        Optional<Appointment> opt = appointmentRepo.findByAppointmentNumber(appointmentNumber);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Appointment appointment = opt.get();

        // 2. Se estamos a tentar mudar o Médico, verificar se o NOVO médico existe na tabela local
        if (patch.getPhysicianNumber() != null) {
            if (!physicianRepo.existsById(patch.getPhysicianNumber())) {
                return ResponseEntity.status(404).body("Novo physicianNumber inexistente (ou não sincronizado)");
            }
            appointment.setPhysicianNumber(patch.getPhysicianNumber());
        }

        // 3. Se estamos a tentar mudar o Paciente, verificar na tabela local
        if (patch.getPatientNumber() != null) {
            if (!patientRepo.existsById(patch.getPatientNumber())) {
                return ResponseEntity.status(404).body("Novo patientNumber inexistente (ou não sincronizado)");
            }
            appointment.setPatientNumber(patch.getPatientNumber());
        }

        // 4. Atualizar restantes campos simples (só se não forem nulos)
        if (patch.getConsultationType() != null) appointment.setConsultationType(patch.getConsultationType());
        if (patch.getStatus() != null) appointment.setStatus(patch.getStatus());
        if (patch.getStartTime() != null) appointment.setStartTime(patch.getStartTime());
        if (patch.getEndTime() != null) appointment.setEndTime(patch.getEndTime());

        // 5. Guardar
        Appointment saved = appointmentRepo.save(appointment);

        return ResponseEntity.ok(saved);
    }
}