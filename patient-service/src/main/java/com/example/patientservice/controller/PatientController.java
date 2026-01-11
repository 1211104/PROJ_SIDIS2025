package com.example.patientservice.controller;

import com.example.patientservice.model.Patient;
import com.example.patientservice.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public List<Patient> getAll() {
        return service.findAll();
    }

    @GetMapping("/number/{patientNumber}")
    public ResponseEntity<Patient> getByNumber(
            @PathVariable String patientNumber,
            @RequestHeader(value = "X-User-Name", required = false) String loggedUser,
            @RequestHeader(value = "X-User-Role", required = false) String loggedRole) {

        // Validação de Segurança (RBAC + Ownership)
        // Se não for ADMIN e o patientNumber da URL for diferente do utilizador logado -> 403
        if (!"ADMIN".equals(loggedRole) && !patientNumber.equals(loggedUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return service.findByNumber(patientNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Patient body) {
        if (body.getPatientNumber() == null || body.getPatientNumber().isBlank()) {
            return ResponseEntity.badRequest().body("patientNumber é obrigatório");
        }
        try {
            Patient saved = service.createPatient(body);
            return ResponseEntity.created(URI.create("/api/patients/number/" + saved.getPatientNumber()))
                    .body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PatchMapping("/number/{patientNumber}")
    public ResponseEntity<Patient> patchByNumber(@PathVariable String patientNumber,
                                                 @RequestBody Patient body) {
        Patient saved = service.updatePatient(patientNumber, body);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/number/{patientNumber}")
    public ResponseEntity<Void> deleteByNumber(@PathVariable String patientNumber) {
        service.deletePatient(patientNumber);
        return ResponseEntity.noContent().build();
    }
}