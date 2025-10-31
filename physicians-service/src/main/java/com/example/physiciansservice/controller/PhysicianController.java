package com.example.physiciansservice.controller;

import com.example.physiciansservice.model.Physician;
import com.example.physiciansservice.repository.PhysicianRepository;
import com.example.physiciansservice.service.PhysicianFanoutService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/physicians")
public class PhysicianController {

    private final PhysicianRepository repository;
    private final PhysicianFanoutService fanout;

    public PhysicianController(PhysicianRepository repository, PhysicianFanoutService fanout) {
        this.repository = repository;
        this.fanout = fanout;
    }

    // =======================
    // PÚBLICOS (com fan-out)
    // =======================

    // Lista/pesquisa agregada (local + peers)
    @GetMapping
    public List<Physician> search(@RequestParam(required = false) String q) {
        return fanout.aggregateSearch(q);
    }

    // Consulta agregada por ID (tenta local, depois peers)
    @GetMapping("/{id}")
    public ResponseEntity<Physician> getById(@PathVariable Long id) {
        return fanout.getByIdWithFanout(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Consulta agregada por physicianNumber (tenta local, depois peers)
    @GetMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Physician> getByNumber(@PathVariable String physicianNumber) {
        return fanout.getByNumberWithFanout(physicianNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Physician body) {
        if (body.getPhysicianNumber() == null || body.getPhysicianNumber().isBlank()) {
            return ResponseEntity.badRequest().body("physicianNumber é obrigatório");
        }
        // verificação global (local + peers)
        if (fanout.existsAnywhereByNumber(body.getPhysicianNumber())) {
            return ResponseEntity.status(409).body("physicianNumber já existe noutra instância");
        }
        Physician saved = repository.save(body);
        return ResponseEntity.created(URI.create("/api/physicians/" + saved.getId())).body(saved);
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

    @PutMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Physician> updateByNumber(@PathVariable String physicianNumber,
                                                    @RequestBody Physician body) {
        return fanout.putByNumberForward(physicianNumber, body);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // INTERNOS (apenas BD local — usados por peers)
    // ==========================================

    @GetMapping("/internal/search")
    public List<Physician> internalSearch(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) return repository.findAll();
        return repository
                .findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(q, q, Pageable.unpaged())
                .getContent();
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<Physician> internalGet(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/internal/by-number/{physicianNumber}")
    public ResponseEntity<Physician> internalGetByNumber(@PathVariable String physicianNumber) {
        return repository.findByPhysicianNumber(physicianNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/internal/by-number/{physicianNumber}")
    public ResponseEntity<Physician> internalUpdateByNumber(@PathVariable String physicianNumber,
                                                            @RequestBody Physician body) {
        return (ResponseEntity<Physician>) repository.findByPhysicianNumber(physicianNumber)
                .map(existing -> {
                    // não permitir trocar a chave de negócio
                    if (body.getPhysicianNumber() != null &&
                            !physicianNumber.equals(body.getPhysicianNumber())) {
                        return ResponseEntity.badRequest().build();
                    }
                    existing.setName(body.getName());
                    existing.setSpecialty(body.getSpecialty());
                    existing.setContactInfo(body.getContactInfo());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @DeleteMapping("/internal/by-number/{physicianNumber}")
    public ResponseEntity<Object> internalDeleteByNumber(@PathVariable String physicianNumber) {
        return repository.findByPhysicianNumber(physicianNumber)
                .map(p -> {
                    repository.delete(p);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // (Opcional) Debug — útil para verificar peers e porta
    @GetMapping("/debug/whoami")
    public Map<String, Object> whoami(@Value("${server.port}") int port,
                                      @Value("${hap.p2p.peers}") List<String> peers) {
        return Map.of("port", port, "peers", peers);
    }

    @DeleteMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Void> deleteByNumber(@PathVariable String physicianNumber) {
        return fanout.deleteByNumberForward(physicianNumber);
    }
}
