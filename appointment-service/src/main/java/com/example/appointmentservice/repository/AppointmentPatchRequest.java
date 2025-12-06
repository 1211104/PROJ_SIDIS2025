package com.example.appointmentservice.repository;

import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.model.ConsultationType;
import java.time.LocalDateTime;

public class AppointmentPatchRequest {

    private String physicianNumber;
    private String patientNumber;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;


    public AppointmentPatchRequest() {
    }

    // Getters e Setters
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

    public ConsultationType getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(ConsultationType consultationType) {
        this.consultationType = consultationType;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}