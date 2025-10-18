package org.example.patient.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entidade que representa um Cliente/Paciente (Clients) do HAP.
 */
@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientNumber;

    private String name;

    private String phoneNumber;

    // Construtor vazio (Requerido pela especificação JPA)
    public Patient() {
    }

    // Construtor com todos os campos (opcional, mas útil)
    public Patient(String patientNumber, String name, String phoneNumber) {
        this.patientNumber = patientNumber;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    // --- GETTERS (Substituem o @Getter do Lombok) ---

    public Long getId() {
        return id;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    // --- SETTERS (Substituem o @Setter do Lombok) ---

    public void setId(Long id) {
        this.id = id;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}