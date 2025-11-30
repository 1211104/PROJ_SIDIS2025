package com.example.appointmentservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhysicianEvent implements Serializable {
    private String physicianNumber;
    private String name;
    private String specialty;
    private String eventType;
}
