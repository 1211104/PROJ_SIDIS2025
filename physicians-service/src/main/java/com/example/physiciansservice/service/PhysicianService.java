package com.example.physiciansservice.service;

import com.example.physiciansservice.event.PhysicianProducer;
import com.example.physiciansservice.model.Physician;
import com.example.physiciansservice.repository.PhysicianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PhysicianService {

    private final PhysicianRepository repository;
    private final PhysicianProducer producer;

    public PhysicianService(PhysicianRepository repository, PhysicianProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    public List<Physician> findAll() {
        return repository.findAll();
    }

    public Optional<Physician> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Physician> findByNumber(String number) {
        return repository.findByPhysicianNumber(number);
    }

    @Transactional
    public Physician createPhysician(Physician physician) {
        // Validação básica
        if (repository.findByPhysicianNumber(physician.getPhysicianNumber()).isPresent()) {
            throw new IllegalArgumentException("Physician já existe com este número.");
        }

        Physician saved = repository.save(physician);
        producer.sendPhysicianCreated(saved);
        return saved;
    }

    @Transactional
    public Physician updatePhysician(Long id, Physician newData) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setPhysicianNumber(newData.getPhysicianNumber());
                    existing.setName(newData.getName());
                    existing.setSpecialty(newData.getSpecialty());
                    existing.setContactInfo(newData.getContactInfo());

                    Physician saved = repository.save(existing);
                    producer.sendPhysicianUpdated(saved);
                    return saved;
                }).orElseThrow(() -> new IllegalArgumentException("Médico não encontrado ID: " + id));
    }

    @Transactional
    public Physician upsertByNumber(String number, Physician data) {
        return repository.findByPhysicianNumber(number)
                .map(existing -> {
                    // Update
                    if (data.getName() != null) existing.setName(data.getName());
                    if (data.getSpecialty() != null) existing.setSpecialty(data.getSpecialty());
                    if (data.getContactInfo() != null) existing.setContactInfo(data.getContactInfo());

                    Physician saved = repository.save(existing);
                    producer.sendPhysicianUpdated(saved);
                    return saved;
                })
                .orElseGet(() -> {
                    // Create
                    Physician newP = new Physician();
                    newP.setPhysicianNumber(number);
                    newP.setName(data.getName());
                    newP.setSpecialty(data.getSpecialty());
                    newP.setContactInfo(data.getContactInfo());
                    return createPhysician(newP); // Reutiliza create (que já envia evento)
                });
    }

    @Transactional
    public void deleteById(Long id) {
        repository.findById(id).ifPresent(p -> {
            repository.delete(p);
            producer.sendPhysicianDeleted(p.getPhysicianNumber());
        });
    }

    @Transactional
    public void deleteByNumber(String number) {
        repository.findByPhysicianNumber(number).ifPresent(p -> {
            repository.delete(p);
            producer.sendPhysicianDeleted(number);
        });
    }
}