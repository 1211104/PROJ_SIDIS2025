package com.example.appointmentservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "external_patients")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalPatient {
    @Id
    private String patientNumber;
    private String name;
}