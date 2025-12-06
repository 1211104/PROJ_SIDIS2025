package com.example.patientservice.event;

import java.io.Serializable;

public class PatientEvent implements Serializable {

    private String patientNumber;
    private String name;
    private String phoneNumber;
    private String eventType;


    public PatientEvent() {
    }


    public PatientEvent(String patientNumber, String name, String phoneNumber, String eventType) {
        this.patientNumber = patientNumber;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.eventType = eventType;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "PatientEvent{" +
                "patientNumber='" + patientNumber + '\'' +
                ", name='" + name + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}