package com.example.patientservice.service;

import com.example.patientservice.event.PatientProducer;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository repository;
    private final PatientProducer producer;

    public PatientService(PatientRepository repository, PatientProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    public List<Patient> findAll() {
        return repository.findAll();
    }

    public Optional<Patient> findByNumber(String number) {
        return repository.findByPatientNumber(number);
    }

    @Transactional
    public Patient createPatient(Patient patient) {
        // Verifica duplicados (Regra de Negócio)
        if (repository.findByPatientNumber(patient.getPatientNumber()).isPresent()) {
            throw new IllegalArgumentException("patientNumber já existe");
        }

        Patient saved = repository.save(patient);
        // Dispara evento para sincronizar as outras réplicas
        producer.sendPatientCreated(saved);
        return saved;
    }

    @Transactional
    public Patient updatePatient(String patientNumber, Patient updateInfo) {
        return repository.findByPatientNumber(patientNumber)
                .map(existing -> {
                    if (updateInfo.getName() != null) existing.setName(updateInfo.getName());
                    if (updateInfo.getPhoneNumber() != null) existing.setPhoneNumber(updateInfo.getPhoneNumber());

                    Patient saved = repository.save(existing);
                    producer.sendPatientUpdated(saved);
                    return saved;
                })
                // Se não existir, cria (Upsert)
                .orElseGet(() -> {
                    Patient newP = new Patient();
                    newP.setPatientNumber(patientNumber);
                    newP.setName(updateInfo.getName());
                    newP.setPhoneNumber(updateInfo.getPhoneNumber());
                    return createPatient(newP); // Reutiliza o método create que já envia evento
                });
    }

    @Transactional
    public void deletePatient(String patientNumber) {
        repository.findByPatientNumber(patientNumber).ifPresent(p -> {
            repository.delete(p);
            producer.sendPatientDeleted(patientNumber);
        });
    }
}