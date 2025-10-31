// controller/AppointmentController.java
package com.example.appointmentservice.controller;

import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.model.ConsultationType;
import com.example.appointmentservice.repository.AppointmentPatchRequest;
import com.example.appointmentservice.repository.AppointmentRepository;
import com.example.appointmentservice.service.AppointmentFanoutService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository repo;
    private final AppointmentFanoutService fanout;

    public AppointmentController(AppointmentRepository repo, AppointmentFanoutService fanout) {
        this.repo = repo;
        this.fanout = fanout;
    }

    // ---------- PÚBLICOS (fan-out de leitura) ----------

    // lista agregada; filtros opcionais
    @GetMapping
    public List<Appointment> search(@RequestParam(required = false) String physician,
                                    @RequestParam(required = false) String patient) {
        return fanout.aggregateSearch(physician, patient);
    }

    // by-number (agregado)
    @GetMapping("/by-number/{appointmentNumber}")
    public ResponseEntity<Appointment> getByNumber(@PathVariable String appointmentNumber) {
        return fanout.getByNumberWithFanout(appointmentNumber)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // POST (sem replicação por agora; valida referências e unicidade global)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Appointment body) {
        if (body.getAppointmentNumber() == null || body.getAppointmentNumber().isBlank())
            return ResponseEntity.badRequest().body("appointmentNumber é obrigatório");
        if (body.getPhysicianNumber() == null || body.getPhysicianNumber().isBlank())
            return ResponseEntity.badRequest().body("physicianNumber é obrigatório");
        if (body.getPatientNumber() == null || body.getPatientNumber().isBlank())
            return ResponseEntity.badRequest().body("patientNumber é obrigatório");

        // unicidade global
        if (fanout.appointmentExistsAnywhere(body.getAppointmentNumber()))
            return ResponseEntity.status(409).body("appointmentNumber já existe noutra instância");

        // valida physician/patient nos serviços externos
        if (!fanout.physicianExists(body.getPhysicianNumber()))
            return ResponseEntity.status(409).body("physicianNumber inexistente");
        if (!fanout.patientExists(body.getPatientNumber()))
            return ResponseEntity.status(409).body("patientNumber inexistente");

        // valores default
        if (body.getStatus() == null) body.setStatus(AppointmentStatus.SCHEDULED);
        if (body.getConsultationType() == null) body.setConsultationType(ConsultationType.IN_PERSON);
        if (body.getStartTime() == null) body.setStartTime(LocalDateTime.now());

        Appointment saved = repo.save(body);
        return ResponseEntity.created(URI.create("/api/appointments/by-number/" + saved.getAppointmentNumber()))
                .body(saved);
    }

    // (opcional) PUT/DELETE locais ou com forward — podemos adicionar depois

    // ---------- INTERNOS (só BD local; usados por peers) ----------

    @GetMapping("/internal/search")
    public List<Appointment> internalSearch(@RequestParam(required = false) String physician,
                                            @RequestParam(required = false) String patient) {
        if (physician != null && !physician.isBlank())
            return repo.findByPhysicianNumber(physician, Pageable.unpaged()).getContent();
        if (patient != null && !patient.isBlank())
            return repo.findByPatientNumber(patient, Pageable.unpaged()).getContent();
        return repo.findAll();
    }

    @GetMapping("/internal/by-number/{appointmentNumber}")
    public ResponseEntity<Appointment> internalByNumber(@PathVariable String appointmentNumber) {
        return repo.findByAppointmentNumber(appointmentNumber)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // (público, com fan-out)
    @DeleteMapping("/by-number/{appointmentNumber}")
    public ResponseEntity<?> deleteByNumber(@PathVariable String appointmentNumber) {
        boolean deleted = fanout.deleteByNumberAnywhere(appointmentNumber);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    //apaga só na BD local (usado pelos peers)
    @DeleteMapping("/internal/by-number/{appointmentNumber}")
    public ResponseEntity<Void> internalDeleteByNumber(@PathVariable String appointmentNumber) {
        var found = repo.findByAppointmentNumber(appointmentNumber);
        if (found.isEmpty()) return ResponseEntity.notFound().build();
        repo.delete(found.get());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/by-number/{appointmentNumber}")
    public ResponseEntity<?> patchByNumber(@PathVariable String appointmentNumber,
                                           @RequestBody AppointmentPatchRequest patch) {
        // tenta local/peer. Se referências inválidas, devolvemos 409
        boolean ok = fanout.patchByNumberAnywhere(appointmentNumber, patch);
        if (ok) return ResponseEntity.noContent().build();

        // distinguir 409 (ref inválida) de 404 (não encontrado) com uma validação explícita
        // se qualquer campo de ref foi pedido e não existe, devolve 409
        if ((patch.getPhysicianNumber() != null && !fanout.physicianExists(patch.getPhysicianNumber())) ||
                (patch.getPatientNumber() != null && !fanout.patientExists(patch.getPatientNumber()))) {
            return ResponseEntity.status(409).body("Referências inválidas (patient/physician)");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * PATCH interno: aplica só na BD local (chamado pelos peers)
     */
    @PatchMapping("/internal/by-number/{appointmentNumber}")
    public ResponseEntity<Void> internalPatch(@PathVariable String appointmentNumber,
                                              @RequestBody AppointmentPatchRequest patch) {
        var opt = repo.findByAppointmentNumber(appointmentNumber);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var a = opt.get();
        // aqui NÃO validamos refs externas (supomos que o peer já validou)
        if (patch.getPhysicianNumber() != null) a.setPhysicianNumber(patch.getPhysicianNumber());
        if (patch.getPatientNumber() != null) a.setPatientNumber(patch.getPatientNumber());
        if (patch.getConsultationType() != null) a.setConsultationType(patch.getConsultationType());
        if (patch.getStatus() != null) a.setStatus(patch.getStatus());
        if (patch.getStartTime() != null) a.setStartTime(patch.getStartTime());
        if (patch.getEndTime() != null) a.setEndTime(patch.getEndTime());
        repo.save(a);
        return ResponseEntity.noContent().build();
    }

}

