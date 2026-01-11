package com.example.appointmentservice.controller;

import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Appointment> getAll() {
        return service.findAll();
    }

    @GetMapping("/by-number/{appointmentNumber}")
    public ResponseEntity<Appointment> getByNumber(@PathVariable String appointmentNumber) {
        return service.findByNumber(appointmentNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Appointment body) {
        try {
            Appointment created = service.createAppointment(body);
            return ResponseEntity.accepted()
                    .location(URI.create("/api/appointments/by-number/" + created.getAppointmentNumber()))
                    .body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage()); // Erro de Médico/Paciente não encontrado
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/by-number/{appointmentNumber}")
    public ResponseEntity<Void> deleteByNumber(@PathVariable String appointmentNumber) {
        service.deleteAppointment(appointmentNumber);
        return ResponseEntity.noContent().build();
    }
}