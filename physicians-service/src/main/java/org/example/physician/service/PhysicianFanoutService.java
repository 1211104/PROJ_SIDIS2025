package org.example.physician.service;

import org.example.physician.model.Physician;
import org.example.physician.p2p.PeerClient;
import org.example.physician.repository.PhysicianRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PhysicianFanoutService {

    private final PhysicianRepository repo;
    private final PeerClient peerClient;
    private final List<String> peers;     // já normalizados/filtrados
    private final String selfBaseUrl;

    public PhysicianFanoutService(PhysicianRepository repo,
                                  PeerClient peerClient,
                                  @Value("${hap.p2p.peers:}") List<String> peerBaseUrls,
                                  @Value("${server.port}") int port) {
        this.repo = repo;
        this.peerClient = peerClient;
        this.selfBaseUrl = "http://localhost:" + port;

        // normalizar e filtrar peers
        this.peers = Optional.ofNullable(peerBaseUrls).orElseGet(List::of).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(PhysicianFanoutService::normalize)     // remove barra final
                .filter(s -> !s.equalsIgnoreCase(normalize(selfBaseUrl))) // não me chamo
                .distinct()
                .collect(Collectors.toList());
    }

    private static String normalize(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public List<Physician> aggregateSearch(String q) {
        // Local
        List<Physician> local = (q == null || q.isBlank())
                ? repo.findAll()
                : repo.findByNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(q, q, Pageable.unpaged())
                .getContent();

        // Peers
        List<Physician> remote = peers.stream()
                .flatMap(url -> peerClient.searchLocal(url, q).stream())
                .collect(Collectors.toList());

        // Merge + dedupe
        Map<String, Physician> merged = new LinkedHashMap<>();
        local.forEach(p -> merged.put(key(p), p));
        remote.forEach(p -> merged.putIfAbsent(key(p), p));

        // (Opcional) log leve
        // System.out.printf("Fan-out search q='%s' peers=%s local=%d remote=%d%n", q, peers, local.size(), remote.size());

        return new ArrayList<>(merged.values());
    }

    public Optional<Physician> getByIdWithFanout(long id) {
        var local = repo.findById(id);
        if (local.isPresent()) return local;

        for (String url : peers) {
            var p = peerClient.getLocalById(url, id);
            if (p != null) return Optional.of(p);
        }
        return Optional.empty();
    }

    public Optional<Physician> getByNumberWithFanout(String physicianNumber) {
        var local = repo.findByPhysicianNumber(physicianNumber);
        if (local.isPresent()) return local;

        for (String url : peers) {
            var p = peerClient.getLocalByNumber(url, physicianNumber);
            if (p != null) return Optional.of(p);
        }
        return Optional.empty();
    }

    private String key(Physician p) {
        return p.getPhysicianNumber() != null ? p.getPhysicianNumber() : String.valueOf(p.getId());
    }

    // PhysicianFanoutService.java
    public boolean existsAnywhereByNumber(String physicianNumber) {
        // local
        if (repo.findByPhysicianNumber(physicianNumber).isPresent()) return true;

        // peers
        for (String url : peers) {
            var p = peerClient.getLocalByNumber(url, physicianNumber);
            if (p != null) return true;
        }
        return false;
    }

}


