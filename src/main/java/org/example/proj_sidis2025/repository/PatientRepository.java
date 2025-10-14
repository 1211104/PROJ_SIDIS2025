package org.example.proj_sidis2025.repository;

import org.example.proj_sidis2025.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Patient findByPatientNumber(String patientNumber);

    boolean existsByPatientNumber(String patientNumber);
}