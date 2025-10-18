package org.example.patient.replication;

import org.example.patient.model.Patient;
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

                        if (response.getStatusCode().is2xxSuccessful()) {
                            System.out.println("Propagação bem-sucedida para a réplica na porta: " + port);
                        } else {
                            System.err.println("Replicação falhou com status " + response.getStatusCode() + " para a porta: " + port);
                        }
                    } catch (Exception e) {
                        System.err.println("Falha ao propagar para a réplica na porta: " + port + ". Erro: " + e.getMessage());
                    }
                });
    }
}