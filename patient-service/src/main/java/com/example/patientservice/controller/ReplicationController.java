package com.example.patientservice.controller;

import com.example.patientservice.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

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

                System.out.println("Replicação interna (INSERT) concluída. ID: " + receivedId);
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

    @PutMapping("/internal-put")
    public ResponseEntity<Patient> internalPut(@RequestBody Patient patient) {
        Long receivedId = patient.getId();

        String updateSql = "UPDATE PATIENT SET name = ?, phone_number = ? WHERE id = ?";

        try {
            int rowsAffected = jdbcTemplate.update(
                    updateSql,
                    patient.getName(),
                    patient.getPhoneNumber(),
                    receivedId
            );

            if (rowsAffected > 0) {
                System.out.println("Replicação interna (UPDATE) concluída. ID: " + receivedId);
                return ResponseEntity.ok(patient);
            } else {
                System.out.println("Replicação interna (UPDATE) aceita: ID " + receivedId + " não encontrado. [Status 202 ACCEPTED]");
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(patient);
            }
        } catch (Exception e) {
            System.err.println("Erro durante a replicação interna (UPDATE) para ID: " + receivedId + ". Erro: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(patient);
        }
    }

    @DeleteMapping("/internal-delete/{id}")
    public ResponseEntity<Void> internalDelete(@PathVariable Long id) {

        String deleteSql = "DELETE FROM PATIENT WHERE id = ?";

        try {
            int rowsAffected = jdbcTemplate.update(deleteSql, id);

            if (rowsAffected > 0) {
                System.out.println("Replicação interna (DELETE) concluída. ID removido: " + id);
                return ResponseEntity.noContent().build();
            } else {
                System.out.println("Replicação interna (DELETE) aceita: ID " + id + " já inexistente. [Status 202 ACCEPTED]");
                return ResponseEntity.status(HttpStatus.ACCEPTED).build();
            }
        } catch (Exception e) {
            System.err.println("Erro durante a replicação interna (DELETE) para ID: " + id + ". Erro: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}