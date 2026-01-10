package com.example.physiciansservice.event.dto;

import java.io.Serializable;

public class PhysicianReservedEvent implements Serializable {
    private String appointmentNumber;
    private String physicianNumber;
    private boolean isAvailable;

    // Construtor Vazio
    public PhysicianReservedEvent() {
    }

    // Construtor Completo
    public PhysicianReservedEvent(String appointmentNumber, String physicianNumber, boolean isAvailable) {
        this.appointmentNumber = appointmentNumber;
        this.physicianNumber = physicianNumber;
        this.isAvailable = isAvailable;
    }

    // Getters e Setters
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

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}