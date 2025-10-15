package org.example.physician.repository;

import org.example.physician.model.Physician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhysicianRepository extends JpaRepository<Physician, Long> {
    Optional<Physician> findByPhysicianNumber(String physicianNumber);
}

