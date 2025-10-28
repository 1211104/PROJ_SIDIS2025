package org.example.patient.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, name = "patient_number")
    private String patientNumber;

    private String name;

    private String phoneNumber;

    public Patient() {
    }

    public Patient(String patientNumber, String name, String phoneNumber) {
        this.patientNumber = patientNumber;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }


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