package com.example.patientservice.replication;

import com.example.patientservice.model.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class ReplicationService {

    @Value("${patient.replicas.ports}")
    private String replicaPorts;

    @Value("${server.port}")
    private int localPort;

    private final RestTemplate restTemplate = new RestTemplate();

    public void propagatePost(Patient patient) {

        List<String> ports = Arrays.asList(replicaPorts.split(","));

        ports.stream()
                .map(String::trim)
                .filter(port -> {
                    try {
                        return Integer.parseInt(port) != localPort;
                    } catch (NumberFormatException e) {
                        System.err.println("Erro de configuração: A porta '" + port + "' não é um número válido.");
                        return false;
                    }
                })
                .forEach(port -> {
                    String url = String.format("http://localhost:%s/api/replication/patients/internal-post", port);

                    try {
                        ResponseEntity<Patient> response = restTemplate.postForEntity(url, patient, Patient.class);

                        if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 202) {
                            System.out.println("Propagação de POST bem-sucedida para a réplica na porta: " + port);
                        } else {
                            System.err.println("Replicação falhou com status " + response.getStatusCode() + " para a porta: " + port);
                        }
                    } catch (Exception e) {
                        System.err.println("Falha ao propagar POST para a réplica na porta: " + port + ". Erro: " + e.getMessage());
                    }
                });
    }

    public void propagatePut(Patient patient) {

        List<String> ports = Arrays.asList(replicaPorts.split(","));

        ports.stream()
                .map(String::trim)
                .filter(port -> {
                    try {
                        return Integer.parseInt(port) != localPort;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .forEach(port -> {
                    String url = String.format("http://localhost:%s/api/replication/patients/internal-put", port);

                    try {
                        restTemplate.put(url, patient);
                        System.out.println("Propagação de PUT bem-sucedida para a réplica na porta: " + port);
                    } catch (Exception e) {
                        System.err.println("Falha ao propagar PUT para a réplica na porta: " + port + ". Erro: " + e.getMessage());
                    }
                });
    }

    public void propagateDelete(Long patientId) {

        List<String> ports = Arrays.asList(replicaPorts.split(","));

        ports.stream()
                .map(String::trim)
                .filter(port -> {
                    try {
                        return Integer.parseInt(port) != localPort;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .forEach(port -> {
                    String url = String.format("http://localhost:%s/api/replication/patients/internal-delete/%d", port, patientId);

                    try {
                        restTemplate.delete(url);
                        System.out.println("Propagação de DELETE bem-sucedida para a réplica na porta: " + port);
                    } catch (Exception e) {
                        System.err.println("Falha ao propagar DELETE para a réplica na porta: " + port + ". Erro: " + e.getMessage());
                    }
                });
    }
}