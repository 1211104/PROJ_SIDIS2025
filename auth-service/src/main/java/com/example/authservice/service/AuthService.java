package com.example.authservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Value("${app.jwt.secret}")
    private String secret;


    public String generateToken(String userName) {
        // Vai buscar o utilizador a BD para saber qual e o Role
        com.example.authservice.model.UserCredential user = repository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado ao gerar token"));

        // Adicionar o Role aos "Claims"
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());

        return createToken(claims, userName);
    }

    private String createToken(Map<String, Object> claims, String userName) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public void validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Autowired
    private com.example.authservice.repository.UserCredentialRepository repository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public String saveUser(com.example.authservice.model.UserCredential credential) {
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        // Se não tiver role, assume PATIENT
        if (credential.getRole() == null) {
            credential.setRole(com.example.authservice.model.UserRole.PATIENT);
        }
        repository.save(credential);
        return "Utilizador adicionado ao sistema";
    }
}