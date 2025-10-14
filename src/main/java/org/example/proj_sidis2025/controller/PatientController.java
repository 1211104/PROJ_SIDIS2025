package org.example.proj_sidis2025.controller;

import org.example.proj_sidis2025.model.Patient;
import org.example.proj_sidis2025.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patientRepository;


    @Autowired
    public PatientController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Patient createPatient(@RequestBody Patient patient) {
        if (patientRepository.existsByPatientNumber(patient.getPatientNumber())) {
            throw new IllegalArgumentException("Patient with number " + patient.getPatientNumber() + " already exists.");
        }
        return patientRepository.save(patient);
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