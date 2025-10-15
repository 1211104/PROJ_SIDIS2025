package org.example.physician.controller;

import org.example.physician.model.Physician;
import org.example.physician.repository.PhysicianRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/physicians")
public class PhysicianController {

    private final PhysicianRepository repository;

    public PhysicianController(PhysicianRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Physician> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Physician> getById(@PathVariable Long id) {
        Optional<Physician> physician = repository.findById(id);
        return physician.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Physician create(@RequestBody Physician physician) {
        return repository.save(physician);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Physician> update(@PathVariable Long id, @RequestBody Physician newPhysician) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setPhysicianNumber(newPhysician.getPhysicianNumber());
                    existing.setName(newPhysician.getName());
                    existing.setSpecialty(newPhysician.getSpecialty());
                    existing.setContactInfo(newPhysician.getContactInfo());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Physician> getByPhysicianNumber(@PathVariable String physicianNumber) {
        return repository.findByPhysicianNumber(physicianNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

