package com.example.physiciansservice.repository;

import com.example.physiciansservice.model.Physician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhysicianRepository extends JpaRepository<Physician, Long> {
    Optional<Physician> findByPhysicianNumber(String physicianNumber);

    // útil para /api/physicians?q=
    Page<Physician> findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
            String name, String specialty, Pageable pageable);
}


