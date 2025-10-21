package org.example.physician.p2p;

import org.example.physician.model.Physician;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public PeerClient(RestTemplate peerRestTemplate) {
        this.rest = peerRestTemplate;
    }

    private static String normalize(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public List<Physician> searchLocal(String baseUrl, String q) {
        try {
            baseUrl = normalize(baseUrl);
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/physicians/internal/search");
            if (q != null && !q.isBlank()) b.queryParam("q", q);
            URI uri = b.build(true).toUri(); // true -> não re-escapa

            var resp = rest.getForEntity(uri, Physician[].class);
            var body = resp.getBody();
            return (body == null) ? List.of() : Arrays.asList(body);
        } catch (Exception e) {
            // log opcional: System.out.println("Peer searchLocal falhou: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Physician getLocalById(String baseUrl, long id) {
        try {
            baseUrl = normalize(baseUrl);
            return rest.getForObject(baseUrl + "/api/physicians/internal/{id}", Physician.class, id);
        } catch (Exception e) {
            return null;
        }
    }

    public Physician getLocalByNumber(String baseUrl, String physicianNumber) {
        try {
            baseUrl = normalize(baseUrl);
            return rest.getForObject(baseUrl + "/api/physicians/internal/by-number/{num}",
                    Physician.class, physicianNumber);
        } catch (Exception e) {
            return null;
        }
    }

    public ResponseEntity<Physician> putLocalByNumber(String baseUrl, String physicianNumber, Physician body) {
        try {
            String url = normalize(baseUrl) + "/api/physicians/internal/by-number/{num}";
            return rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(body), Physician.class, physicianNumber);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }


    public ResponseEntity<Void> deleteLocalByNumber(String baseUrl, String physicianNumber) {
        try {
            String url = normalize(baseUrl) + "/api/physicians/internal/by-number/{num}";
            return rest.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class, physicianNumber);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}


