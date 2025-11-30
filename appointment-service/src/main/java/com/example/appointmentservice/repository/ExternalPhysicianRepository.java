package com.example.appointmentservice.repository;

import com.example.appointmentservice.model.ExternalPhysician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalPhysicianRepository extends JpaRepository<ExternalPhysician, String> {

}