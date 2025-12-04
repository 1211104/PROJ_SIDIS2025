package com.example.physiciansservice.controller;

import com.example.physiciansservice.event.PhysicianProducer;
import com.example.physiciansservice.model.Physician;
import com.example.physiciansservice.repository.PhysicianRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/physicians")
public class PhysicianController {

    private final PhysicianRepository repository;
    private final PhysicianProducer producer;

    public PhysicianController(PhysicianRepository repository, PhysicianProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }


    // LEITURAS (Apenas Local)

    @GetMapping
    public List<Physician> search(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return repository.findAll();
        }
        return repository
                .findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(q, q, Pageable.unpaged())
                .getContent();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Physician> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Physician> getByNumber(@PathVariable String physicianNumber) {
        return repository.findByPhysicianNumber(physicianNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // ESCRITAS (Gravam Local + Enviam Evento)

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Physician body) {
        if (body.getPhysicianNumber() == null || body.getPhysicianNumber().isBlank()) {
            return ResponseEntity.badRequest().body("physicianNumber é obrigatório");
        }

        // Validação de Duplicados (Apenas na BD Local)
        if (repository.findByPhysicianNumber(body.getPhysicianNumber()).isPresent()) {
            return ResponseEntity.status(409).body("physicianNumber já existe nesta instância");
        }

        // Gravar na Base de Dados Local (Persistência)
        Physician saved = repository.save(body);

        // RABBITMQ: Avisa que foi criado um physician
        // Sincroniza a outra réplica e para o Appointment Service
        producer.sendPhysicianCreated(saved);

        return ResponseEntity.created(URI.create("/api/physicians/" + saved.getId())).body(saved);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Physician> update(@PathVariable Long id, @RequestBody Physician newPhysician) {
        return repository.findById(id)
                .map(existing -> {
                    // Atualiza Localmente
                    existing.setPhysicianNumber(newPhysician.getPhysicianNumber());
                    existing.setName(newPhysician.getName());
                    existing.setSpecialty(newPhysician.getSpecialty());
                    existing.setContactInfo(newPhysician.getContactInfo());
                    Physician saved = repository.save(existing);

                    // Envia evento UPDATED
                    producer.sendPhysicianUpdated(saved);

                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Physician> patchByNumber(@PathVariable String physicianNumber,
                                                   @RequestBody Physician body) {
        return repository.findByPhysicianNumber(physicianNumber)
                .map(existing -> {
                    // --- CENÁRIO A: JÁ EXISTE ---

                    if (body.getName() != null) existing.setName(body.getName());
                    if (body.getSpecialty() != null) existing.setSpecialty(body.getSpecialty());
                    if (body.getContactInfo() != null) existing.setContactInfo(body.getContactInfo());

                    Physician saved = repository.save(existing);

                    // Envia evento UPDATE
                    producer.sendPhysicianUpdated(saved);

                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> {
                    // --- CENÁRIO B: NÃO EXISTE (Lógica CREATE / UPSERT) ---

                    Physician newPhysician = new Physician();
                    newPhysician.setPhysicianNumber(physicianNumber);

                    newPhysician.setName(body.getName());
                    newPhysician.setSpecialty(body.getSpecialty());
                    newPhysician.setContactInfo(body.getContactInfo());

                    Physician saved = repository.save(newPhysician);

                    // Envia evento CREATED
                    producer.sendPhysicianCreated(saved);

                    return ResponseEntity.created(URI.create("/api/physicians/by-number/" + saved.getPhysicianNumber()))
                            .body(saved);
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();

        // physicianNumber necessário antes de apagar
        Physician p = repository.findById(id).get();

        // Apaga Localmente
        repository.deleteById(id);

        // Envia evento DELETED
        producer.sendPhysicianDeleted(p.getPhysicianNumber());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-number/{physicianNumber}")
    public ResponseEntity<Void> deleteByNumber(@PathVariable String physicianNumber) {
        var opt = repository.findByPhysicianNumber(physicianNumber);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        // Apaga Localmente
        repository.delete(opt.get());

        // Envia evento DELETED
        producer.sendPhysicianDeleted(physicianNumber);

        return ResponseEntity.noContent().build();
    }


}