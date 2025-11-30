package com.example.appointmentservice.repository;

import com.example.appointmentservice.model.ExternalPatient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalPatientRepository extends JpaRepository<ExternalPatient, String> {
}