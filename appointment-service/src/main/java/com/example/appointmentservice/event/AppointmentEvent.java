package com.example.appointmentservice.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AppointmentEvent implements Serializable {

    private String appointmentNumber;
    private String physicianNumber;
    private String patientNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String consultationType;
    private String eventType;

    public AppointmentEvent() {
    }


    public AppointmentEvent(String appointmentNumber, String physicianNumber, String patientNumber,
                            LocalDateTime startTime, LocalDateTime endTime, String status,
                            String consultationType, String eventType) {
        this.appointmentNumber = appointmentNumber;
        this.physicianNumber = physicianNumber;
        this.patientNumber = patientNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.consultationType = consultationType;
        this.eventType = eventType;
    }

    // Getters e Setters
    public String getAppointmentNumber() { return appointmentNumber; }
    public void setAppointmentNumber(String appointmentNumber) { this.appointmentNumber = appointmentNumber; }

    public String getPhysicianNumber() { return physicianNumber; }
    public void setPhysicianNumber(String physicianNumber) { this.physicianNumber = physicianNumber; }

    public String getPatientNumber() { return patientNumber; }
    public void setPatientNumber(String patientNumber) { this.patientNumber = patientNumber; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConsultationType() { return consultationType; }
    public void setConsultationType(String consultationType) { this.consultationType = consultationType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}