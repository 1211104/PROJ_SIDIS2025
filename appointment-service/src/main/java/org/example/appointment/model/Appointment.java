package org.example.appointment.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment", uniqueConstraints = @UniqueConstraint(columnNames = "appointment_number"))
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="appointment_number", nullable=false, unique=true)
    private String appointmentNumber;

    @Column(name="physician_number", nullable=false)
    private String physicianNumber;

    @Column(name="patient_number", nullable=false)
    private String patientNumber;

    @Enumerated(EnumType.STRING)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Appointment() {}

    public Appointment(String appointmentNumber, String physicianNumber, String patientNumber,
                       ConsultationType consultationType, AppointmentStatus status,
                       LocalDateTime startTime, LocalDateTime endTime) {
        this.appointmentNumber = appointmentNumber;
        this.physicianNumber = physicianNumber;
        this.patientNumber = patientNumber;
        this.consultationType = consultationType;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // getters & setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

