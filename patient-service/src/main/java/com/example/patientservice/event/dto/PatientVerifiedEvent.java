package com.example.patientservice.event.dto;

import java.io.Serializable;

public class PatientVerifiedEvent implements Serializable {
    private String appointmentNumber;
    private String patientNumber;
    private boolean isVerified;

    public PatientVerifiedEvent() {
    }

    public PatientVerifiedEvent(String appointmentNumber, String patientNumber, boolean isVerified) {
        this.appointmentNumber = appointmentNumber;
        this.patientNumber = patientNumber;
        this.isVerified = isVerified;
    }

    // Getters e Setters
    public String getAppointmentNumber() { return appointmentNumber; }
    public void setAppointmentNumber(String appointmentNumber) { this.appointmentNumber = appointmentNumber; }

    public String getPatientNumber() { return patientNumber; }
    public void setPatientNumber(String patientNumber) { this.patientNumber = patientNumber; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}