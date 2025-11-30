package com.example.patientservice.controller;

import com.example.patientservice.event.PatientProducer;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository repository;
    private final PatientProducer producer;

    public PatientController(PatientRepository repository, PatientProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @GetMapping
    public List<Patient> getAll() {
        return repository.findAll();
    }

    @GetMapping("/number/{patientNumber}")
    public ResponseEntity<Patient> getByNumber(@PathVariable String patientNumber) {
        return repository.findByPatientNumber(patientNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Patient body) {
        if (body.getPatientNumber() == null || body.getPatientNumber().isBlank()) {
            return ResponseEntity.badRequest().body("patientNumber é obrigatório");
        }
        if (repository.findByPatientNumber(body.getPatientNumber()).isPresent()) {
            return ResponseEntity.status(409).body("patientNumber já existe");
        }

        Patient saved = repository.save(body);
        producer.sendPatientCreated(saved);

        return ResponseEntity.created(URI.create("/api/patients/number/" + saved.getPatientNumber()))
                .body(saved);
    }

    @PatchMapping("/number/{patientNumber}")
    @Transactional
    public ResponseEntity<Patient> patchByNumber(@PathVariable String patientNumber,
                                                 @RequestBody Patient body) {
        return repository.findByPatientNumber(patientNumber)
                .map(existing -> {
                    // Update
                    if (body.getName() != null) existing.setName(body.getName());
                    if (body.getPhoneNumber() != null) existing.setPhoneNumber(body.getPhoneNumber());

                    Patient saved = repository.save(existing);
                    producer.sendPatientUpdated(saved);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> {
                    // Create (Upsert)
                    Patient newP = new Patient();
                    newP.setPatientNumber(patientNumber);
                    newP.setName(body.getName());
                    newP.setPhoneNumber(body.getPhoneNumber());

                    Patient saved = repository.save(newP);
                    producer.sendPatientCreated(saved);

                    return ResponseEntity.created(URI.create("/api/patients/number/" + saved.getPatientNumber()))
                            .body(saved);
                });
    }

    @DeleteMapping("/number/{patientNumber}")
    @Transactional
    public ResponseEntity<Void> deleteByNumber(@PathVariable String patientNumber) {
        var opt = repository.findByPatientNumber(patientNumber);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        repository.delete(opt.get());
        producer.sendPatientDeleted(patientNumber);

        return ResponseEntity.noContent().build();
    }
}