package com.example.physiciansservice.controller;

import com.example.physiciansservice.model.Physician;
import com.example.physiciansservice.service.PhysicianService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/physicians")
public class PhysicianController {

    private final PhysicianService service;
    private final com.example.physiciansservice.repository.PhysicianRepository repository;

    public PhysicianController(PhysicianService service, com.example.physiciansservice.repository.PhysicianRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping
    public List<Physician> search(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return service.findAll();
        }
        return repository.findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(q, q, Pageable.unpaged()).getContent();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Physician> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Physician> getByNumber(@PathVariable String physicianNumber) {
        return service.findByNumber(physicianNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Physician body) {
        try {
            Physician saved = service.createPhysician(body);
            return ResponseEntity.created(URI.create("/api/physicians/" + saved.getId())).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Physician> update(@PathVariable Long id, @RequestBody Physician newPhysician) {
        try {
            return ResponseEntity.ok(service.updatePhysician(id, newPhysician));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Physician> patchByNumber(@PathVariable String physicianNumber,
                                                   @RequestBody Physician body) {
        Physician saved = service.upsertByNumber(physicianNumber, body);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Void> deleteByNumber(@PathVariable String physicianNumber) {
        service.deleteByNumber(physicianNumber);
        return ResponseEntity.noContent().build();
    }
}