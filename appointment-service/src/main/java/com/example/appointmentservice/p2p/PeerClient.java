package com.example.appointmentservice.p2p;

import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.repository.AppointmentPatchRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class PeerClient {

    private final RestTemplate rest;

    public PeerClient(@Qualifier("peerRest") RestTemplate peerRestTemplate) {
        this.rest = peerRestTemplate;
    }

    private static String normalize(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public List<Appointment> searchLocal(String baseUrl, String physician, String patient) {
        try {
            var b = UriComponentsBuilder.fromHttpUrl(
                    normalize(baseUrl) + "/api/appointments/internal/search");
            if (physician != null && !physician.isBlank()) b.queryParam("physician", physician);
            if (patient != null && !patient.isBlank()) b.queryParam("patient", patient);
            URI uri = b.build(true).toUri();
            var resp = rest.getForEntity(uri, Appointment[].class);
            var body = resp.getBody();
            return body == null ? List.of() : Arrays.asList(body);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Appointment getLocalByNumber(String baseUrl, String apptNumber) {
        try {
            return rest.getForObject(
                    normalize(baseUrl) + "/api/appointments/internal/by-number/{num}",
                    Appointment.class, apptNumber);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean deleteLocalByNumber(String baseUrl, String apptNumber) {
        try {
            rest.delete(normalize(baseUrl) + "/api/appointments/internal/by-number/{num}", apptNumber);
            return true; // 204/200 = apagou
        } catch (Exception e) {
            return false; // 404/peer down -> não apagou
        }
    }

    public boolean patchLocalByNumber(String baseUrl, String apptNumber, AppointmentPatchRequest patch) {
        try {
            String url = normalize(baseUrl) + "/api/appointments/internal/by-number/{num}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AppointmentPatchRequest> req = new HttpEntity<>(patch, headers);

            ResponseEntity<Void> resp = rest.exchange(url, HttpMethod.PATCH, req, Void.class, apptNumber);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
