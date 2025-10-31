package org.example.appointment.repository;

import org.example.appointment.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    Page<Appointment> findByPhysicianNumber(String physicianNumber, Pageable p);

    Page<Appointment> findByPatientNumber(String patientNumber, Pageable p);
}

