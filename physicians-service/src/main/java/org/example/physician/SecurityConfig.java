package org.example.physician;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())                    // importante p/ POST/PUT/DELETE
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/**").authenticated()    // API pede auth
                        .anyRequest().permitAll()
                )
                .headers(h -> h.frameOptions(f -> f.disable()))
                .httpBasic(Customizer.withDefaults())            // Basic Auth
                .build();
    }
}

