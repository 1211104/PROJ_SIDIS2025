package com.example.apigateway.filter;

import com.example.apigateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String PARTNER_API_KEY = "chave-secreta-parceiro-2026";

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Validar se a rota necessita de segurança
            if (validator.isSecured.test(request)) {

                String role = "UNKNOWN";
                String username = "UNKNOWN";
                boolean isAuthenticated = false;

                // CENÁRIO A: Acesso via API Key (B2B / Parceiros)
                if (request.getHeaders().containsKey("x-api-key")) {
                    String apiKey = request.getHeaders().get("x-api-key").get(0);
                    if (PARTNER_API_KEY.equals(apiKey)) {
                        role = "PARTNER_SYSTEM";
                        username = "External_System";
                        isAuthenticated = true;
                    } else {
                        return onError(exchange, "API Key Inválida", HttpStatus.FORBIDDEN);
                    }
                }

                // CENÁRIO B: Acesso via JWT (Utilizadores Normais)
                else if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        authHeader = authHeader.substring(7);
                    }
                    try {
                        jwtUtil.validateToken(authHeader);
                        role = jwtUtil.getRole(authHeader);
                        username = jwtUtil.extractUsername(authHeader);
                        isAuthenticated = true;
                    } catch (Exception e) {
                        return onError(exchange, "Token JWT Inválido ou Expirado", HttpStatus.FORBIDDEN);
                    }
                }

                // Se não entrou nem por Key nem por Token -> Erro
                if (!isAuthenticated) {
                    return onError(exchange, "Cabeçalho de Autenticação em falta", HttpStatus.UNAUTHORIZED);
                }

                // REGRAS GERAIS DE SEGURANÇA (RBAC)

                String method = request.getMethod().name();
                String path = request.getPath().toString();

                // Bloquear DELETE (Apenas ADMIN pode)
                // Nota: O (PARTNER_SYSTEM) também é bloqueado
                if (method.equals("DELETE") && !"ADMIN".equals(role)) {
                    return onError(exchange, "Acesso Negado: Apenas ADMIN pode apagar recursos", HttpStatus.FORBIDDEN);
                }

                // Bloquear Criação de Médicos (Apenas ADMIN)
                if (path.contains("/api/physicians") && method.equals("POST") && !"ADMIN".equals(role)) {
                    return onError(exchange, "Acesso Negado: Apenas administradores podem registar médicos", HttpStatus.FORBIDDEN);
                }


                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Name", username)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            // Se a rota for pública (ex: /auth/login), deixa passar direto
            return chain.filter(exchange);
        });
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange, String err, HttpStatus status) {
        System.out.println("Bloqueio Gateway: " + err);
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}