package com.example.appointmentservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "external_physicians")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalPhysician {
    @Id
    private String physicianNumber; // PK
    private String name;
}