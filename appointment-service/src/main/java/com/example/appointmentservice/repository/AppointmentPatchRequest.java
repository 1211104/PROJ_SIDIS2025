package com.example.appointmentservice.repository;

import com.example.appointmentservice.model.AppointmentStatus;
import com.example.appointmentservice.model.ConsultationType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentPatchRequest {

    private String physicianNumber;
    private String patientNumber;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}