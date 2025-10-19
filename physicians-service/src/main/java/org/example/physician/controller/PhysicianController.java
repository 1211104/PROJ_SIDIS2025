package org.example.physician.controller;

import org.example.physician.model.Physician;
import org.example.physician.repository.PhysicianRepository;
import org.example.physician.service.PhysicianFanoutService;
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

    // Escrita continua a ser local (sem fan-out)
    @PostMapping
    public ResponseEntity<Physician> create(@RequestBody Physician body) {
        Physician saved = repository.save(body);
        return ResponseEntity.created(URI.create("/api/physicians/" + saved.getId())).body(saved);
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

    // (Opcional) Debug — útil para verificar peers e porta
    @GetMapping("/debug/whoami")
    public Map<String, Object> whoami(@Value("${server.port}") int port,
                                      @Value("${hap.p2p.peers}") List<String> peers) {
        return Map.of("port", port, "peers", peers);
    }
}
