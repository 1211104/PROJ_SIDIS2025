package com.example.physiciansservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.*;

@Entity
public class Physician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String physicianNumber;

    private String name;
    private String specialty;
    private String contactInfo;

    public Physician() {
    }

    public Physician(String physicianNumber, String name, String specialty, String contactInfo) {
        this.physicianNumber = physicianNumber;
        this.name = name;
        this.specialty = specialty;
        this.contactInfo = contactInfo;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getPhysicianNumber() {
        return physicianNumber;
    }

    public String getName() {
        return name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setPhysicianNumber(String physicianNumber) {
        this.physicianNumber = physicianNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
}

