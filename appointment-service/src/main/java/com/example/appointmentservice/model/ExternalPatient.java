package com.example.appointmentservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "external_patients")
public class ExternalPatient {

    @Id
    private String patientNumber;
    private String name;


    public ExternalPatient() {
    }


    public ExternalPatient(String patientNumber, String name) {
        this.patientNumber = patientNumber;
        this.name = name;
    }

    // 3. Getters e Setters
    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}