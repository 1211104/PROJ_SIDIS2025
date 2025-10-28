package org.example.appointment.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean("peerRest")
    public RestTemplate peerRest(RestTemplateBuilder b,
                                 @Value("${hap.p2p.timeout.ms:800}") long t) {
        return b.connectTimeout(Duration.ofMillis(t)).readTimeout(Duration.ofMillis(t))
                .build();
    }

    @Bean("externalRest")
    public RestTemplate externalRest(RestTemplateBuilder b,
                                     @Value("${external.auth.user:admin}") String user,
                                     @Value("${external.auth.pass:admin123}") String pass,
                                     @Value("${hap.external.timeout.ms:1200}") long t) {
        return b.basicAuthentication(user, pass).connectTimeout(Duration.ofMillis(t)).readTimeout(Duration.ofMillis(t))
                .build();
    }
}
