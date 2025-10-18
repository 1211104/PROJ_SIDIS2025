package org.example.patient.controller;

import org.example.patient.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replication/patients")
public class ReplicationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/internal-post")
    public ResponseEntity<Patient> internalPost(@RequestBody Patient patient) {
        Long receivedId = patient.getId();

        try {
            String insertSql = "INSERT INTO PATIENT (id, patient_number, name, phone_number) VALUES (?, ?, ?, ?)";

            int rowsAffected = jdbcTemplate.update(
                    insertSql,
                    receivedId,
                    patient.getPatientNumber(),
                    patient.getName(),
                    patient.getPhoneNumber()
            );

            if (rowsAffected > 0) {
                String seqUpdateSql = String.format(
                        "ALTER TABLE PATIENT ALTER COLUMN ID RESTART WITH %d",
                        receivedId + 1
                );
                jdbcTemplate.execute(seqUpdateSql);

                System.out.println("Replicação interna concluída e sequence H2 atualizada para: " + (receivedId + 1));
                return ResponseEntity.ok(patient);
            }

            throw new IllegalStateException("Native INSERT returned 0 rows affected.");

        } catch (DataIntegrityViolationException e) {
            System.err.println("Conflito de PK/UK (ID já existe) na replicação: " + receivedId + ". [Status 202 ACCEPTED]");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(patient);

        } catch (Exception e) {
            System.err.println("Erro INESPERADO durante a replicação interna para ID: " + receivedId + ". Erro: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(patient);
        }
    }
}