package com.example.patientservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientEvent implements Serializable {
    private String patientNumber;
    private String name;
    private String phoneNumber;
    private String eventType; // "CREATED", "UPDATED", "DELETED"
}
