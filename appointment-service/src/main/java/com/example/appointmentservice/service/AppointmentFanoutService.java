// service/AppointmentFanoutService.java
package com.example.appointmentservice.service;

import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.p2p.PeerClient;
import com.example.appointmentservice.repository.AppointmentPatchRequest;
import com.example.appointmentservice.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentFanoutService {

    private final AppointmentRepository repo;
    private final PeerClient peerClient;
    private final RestTemplate externalRest;
    private final List<String> peers;              // réplicas do appointments
    private final List<String> physicianUrls;      // serviços physicians
    private final List<String> patientUrls;        // serviços patients
    private final String selfBaseUrl;

    public AppointmentFanoutService(AppointmentRepository repo,
                                    PeerClient peerClient,
                                    @Qualifier("externalRest") RestTemplate externalRest,
                                    @Value("${hap.p2p.peers:}") String peersProp,
                                    @Value("${hap.services.physicians:}") String physiciansProp,
                                    @Value("${hap.services.patients:}") String patientsProp,
                                    @Value("${server.port}") int port) {
        this.repo = repo;
        this.peerClient = peerClient;
        this.externalRest = externalRest;
        this.selfBaseUrl = "http://localhost:" + port;

        this.peers = splitCsv(peersProp).stream()
                .map(this::norm)
                .filter(p -> !p.equalsIgnoreCase(norm(selfBaseUrl)))
                .distinct()
                .toList();

        this.physicianUrls = splitCsv(physiciansProp).stream().map(this::norm).toList();
        this.patientUrls = splitCsv(patientsProp).stream().map(this::norm).toList();
    }

    private static List<String> splitCsv(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim).filter(v -> !v.isEmpty()).toList();
    }

    private String norm(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    // --- Fan-out (leituras)
    public List<Appointment> aggregateSearch(String physician, String patient) {
        List<Appointment> local;
        if (physician != null && !physician.isBlank())
            local = repo.findByPhysicianNumber(physician, Pageable.unpaged()).getContent();
        else if (patient != null && !patient.isBlank())
            local = repo.findByPatientNumber(patient, Pageable.unpaged()).getContent();
        else
            local = repo.findAll();

        List<Appointment> remote = peers.stream()
                .flatMap(p -> peerClient.searchLocal(p, physician, patient).stream())
                .collect(Collectors.toList());

        // dedupe por appointmentNumber
        Map<String, Appointment> merged = new LinkedHashMap<>();
        local.forEach(a -> merged.put(a.getAppointmentNumber(), a));
        remote.forEach(a -> merged.putIfAbsent(a.getAppointmentNumber(), a));
        return new ArrayList<>(merged.values());
    }

    public Optional<Appointment> getByNumberWithFanout(String apptNumber) {
        var local = repo.findByAppointmentNumber(apptNumber);
        if (local.isPresent()) return local;
        for (String url : peers) {
            var a = peerClient.getLocalByNumber(url, apptNumber);
            if (a != null) return Optional.of(a);
        }
        return Optional.empty();
    }

    // --- Verificações externas (patients/physicians)
    public boolean physicianExists(String physicianNumber) {
        for (String base : physicianUrls) {
            try {
                var r = externalRest.getForEntity(base + "/api/physicians/by-number/" + physicianNumber, String.class);
                if (r.getStatusCode().is2xxSuccessful()) return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public boolean patientExists(String patientNumber) {
        for (String base : patientUrls) {
            try {
                var r = externalRest.getForEntity(base + "/api/patients/number/" + patientNumber, String.class);
                if (r.getStatusCode().is2xxSuccessful()) return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public boolean appointmentExistsAnywhere(String apptNumber) {
        if (repo.findByAppointmentNumber(apptNumber).isPresent()) return true;
        for (String url : peers) {
            var a = peerClient.getLocalByNumber(url, apptNumber);
            if (a != null) return true;
        }
        return false;
    }

    public boolean deleteByNumberAnywhere(String apptNumber) {
        // tenta local primeiro
        var local = repo.findByAppointmentNumber(apptNumber);
        if (local.isPresent()) {
            repo.delete(local.get());
            return true;
        }
        // tenta nos peers
        for (String url : peers) {
            var a = peerClient.getLocalByNumber(url, apptNumber);
            if (a != null) {
                // manda o peer apagar
                if (peerClient.deleteLocalByNumber(url, apptNumber)) return true;
            }
        }
        return false;
    }

    public boolean patchByNumberAnywhere(String apptNumber, AppointmentPatchRequest p) {
        var local = repo.findByAppointmentNumber(apptNumber);
        if (local.isPresent()) {
            // validar referências se forem alteradas
            if (p.getPhysicianNumber() != null && !physicianExists(p.getPhysicianNumber()))
                return false; // controller devolverá 409
            if (p.getPatientNumber() != null && !patientExists(p.getPatientNumber()))
                return false;

            var a = local.get();
            applyPatch(a, p);
            repo.save(a);
            return true;
        }
        // não está local: tenta nos peers
        for (String url : peers) {
            var a = peerClient.getLocalByNumber(url, apptNumber);
            if (a != null) {
                // valida antes de reencaminhar
                if (p.getPhysicianNumber() != null && !physicianExists(p.getPhysicianNumber()))
                    return false;
                if (p.getPatientNumber() != null && !patientExists(p.getPatientNumber()))
                    return false;

                return peerClient.patchLocalByNumber(url, apptNumber, p);
            }
        }
        return false;
    }

    private void applyPatch(Appointment a, AppointmentPatchRequest p) {
        if (p.getPhysicianNumber() != null) a.setPhysicianNumber(p.getPhysicianNumber());
        if (p.getPatientNumber() != null) a.setPatientNumber(p.getPatientNumber());
        if (p.getConsultationType() != null) a.setConsultationType(p.getConsultationType());
        if (p.getStatus() != null) a.setStatus(p.getStatus());
        if (p.getStartTime() != null) a.setStartTime(p.getStartTime());
        if (p.getEndTime() != null) a.setEndTime(p.getEndTime());
    }

}

