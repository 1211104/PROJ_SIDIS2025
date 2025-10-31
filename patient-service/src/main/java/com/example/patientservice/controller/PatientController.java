package com.example.patientservice.controller;

import com.example.patientservice.replication.ReplicationService;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patientRepository;
    private final ReplicationService replicationService;


    @Autowired
    public PatientController(PatientRepository patientRepository, ReplicationService replicationService) {
        this.patientRepository = patientRepository;
        this.replicationService = replicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> createPatient(@RequestBody Patient patient) {

        try {
            Patient savedPatient = patientRepository.save(patient);

            try {
                replicationService.propagatePost(savedPatient);
            } catch (Exception e) {
                System.err.println("Falha ao replicar POST para peers: " + e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(savedPatient);

        } catch (DataIntegrityViolationException e) {
            String errorMessage = "O número de paciente '" + patient.getPatientNumber() + "' já existe. A criação foi rejeitada.";
            System.err.println(errorMessage);

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno inesperado: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        Optional<Patient> patient = patientRepository.findById(id);

        return patient.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{patientNumber}")
    public ResponseEntity<Patient> getPatientByNumber(@PathVariable String patientNumber) {
        Patient patient = patientRepository.findByPatientNumber(patientNumber);

        if (patient != null) {
            return ResponseEntity.ok(patient);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(@PathVariable Long id, @RequestBody Patient patientDetails) {
        return patientRepository.findById(id)
                .map(patient -> {
                    patient.setName(patientDetails.getName());
                    patient.setPhoneNumber(patientDetails.getPhoneNumber());

                    Patient updatedPatient = patientRepository.save(patient);
                    return ResponseEntity.ok(updatedPatient);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}