package com.example.appointmentservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "external_physicians")
public class ExternalPhysician {

    @Id
    private String physicianNumber;
    private String name;


    public ExternalPhysician() {
    }


    public ExternalPhysician(String physicianNumber, String name) {
        this.physicianNumber = physicianNumber;
        this.name = name;
    }

    // 3. Getters e Setters
    public String getPhysicianNumber() {
        return physicianNumber;
    }

    public void setPhysicianNumber(String physicianNumber) {
        this.physicianNumber = physicianNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}