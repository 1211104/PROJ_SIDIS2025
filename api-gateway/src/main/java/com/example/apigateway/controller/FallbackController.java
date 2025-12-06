package com.example.apigateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<String> authFallback() {
        return Mono.just("O serviço de Autenticação está temporariamente indisponível.");
    }

    @RequestMapping("/fallback/general")
    public Mono<String> generalFallback() {
        return Mono.just("O serviço está a demorar muito tempo ou está indisponível.");
    }
}