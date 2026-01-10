package com.example.physiciansservice.event.dto;

import java.io.Serializable;

public class AppointmentEventDTO implements Serializable {
    private String appointmentNumber;
    private String physicianNumber;
    private String patientNumber;

    public AppointmentEventDTO() {
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public String getPhysicianNumber() {
        return physicianNumber;
    }

    public void setPhysicianNumber(String physicianNumber) {
        this.physicianNumber = physicianNumber;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }
}