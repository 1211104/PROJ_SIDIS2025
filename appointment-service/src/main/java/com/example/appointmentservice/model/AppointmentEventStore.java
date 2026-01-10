package com.example.appointmentservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_events")
public class AppointmentEventStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String appointmentNumber; // O Aggregate ID
    private String eventType;         // Ex: "CREATED", "CONFIRMED"

    @Column(length = 4000)            // Aumentar tamanho para caber o JSON
    private String payload;

    private LocalDateTime occurredAt;


    public AppointmentEventStore() {
    }

    // CONSTRUTOR
    public AppointmentEventStore(String appointmentNumber, String eventType, String payload) {
        this.appointmentNumber = appointmentNumber;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = LocalDateTime.now();
    }


    // GETTERS E SETTERS
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}