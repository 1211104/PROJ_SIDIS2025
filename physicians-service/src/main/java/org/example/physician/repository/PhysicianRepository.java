package org.example.physician.repository;

import org.example.physician.model.Physician;
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


