package org.example.patient;

import org.example.patient.model.Patient;
import org.example.patient.repository.PatientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final PatientRepository patientRepository;

    public DataInitializer(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (patientRepository.count() == 0) {
            System.out.println("Carregando Mock Data Inicial...");

            Patient p1 = new Patient();
            p1.setPatientNumber("P9000");
            p1.setName("Cliente Mock 1");
            p1.setPhoneNumber("910000001");

            Patient p2 = new Patient();
            p2.setPatientNumber("P9001");
            p2.setName("Cliente Mock 2");
            p2.setPhoneNumber("910000002");

            patientRepository.saveAll(List.of(p1, p2));
            System.out.println("Mock Data carregado. Próximos inserts continuarão a partir do ID 3.");
        }
    }
}