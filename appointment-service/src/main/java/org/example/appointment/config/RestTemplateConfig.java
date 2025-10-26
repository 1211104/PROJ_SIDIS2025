package org.example.appointment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // P2P entre réplicas (sem auth)
    @Bean(name = "peerRest")
    @Primary
    public RestTemplate peerRestTemplate(RestTemplateBuilder b,
                                         @Value("${hap.p2p.timeout.ms:1200}") long t) {
        return b.connectTimeout(Duration.ofMillis(t))
                .readTimeout(Duration.ofMillis(t))
                .build();
    }

    // Serviços externos (physicians/patients) com Basic Auth
    @Bean(name = "externalRest")
    public RestTemplate externalRestTemplate(RestTemplateBuilder b,
                                             @Value("${hap.p2p.timeout.ms:1200}") long t,
                                             @Value("${external.auth.user:}") String user,
                                             @Value("${external.auth.pass:}") String pass) {
        var rb = b.connectTimeout(Duration.ofMillis(t)).readTimeout(Duration.ofMillis(t));
        if (user != null && !user.isBlank()) rb = rb.basicAuthentication(user, pass);
        return rb.build();
    }
}
