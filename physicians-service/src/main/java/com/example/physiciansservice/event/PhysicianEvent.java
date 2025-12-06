package com.example.physiciansservice.event;

import java.io.Serializable;

public class PhysicianEvent implements Serializable {

    private String physicianNumber;
    private String name;
    private String specialty;
    private String contactInfo;
    private String eventType;

    public PhysicianEvent() {
    }


    public PhysicianEvent(String physicianNumber, String name, String specialty, String contactInfo, String eventType) {
        this.physicianNumber = physicianNumber;
        this.name = name;
        this.specialty = specialty;
        this.contactInfo = contactInfo;
        this.eventType = eventType;
    }

    // Getters e Setters
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

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }


    @Override
    public String toString() {
        return "PhysicianEvent{" +
                "physicianNumber='" + physicianNumber + '\'' +
                ", name='" + name + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}