package org.example.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.example.appointment")
public class AppointmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }

}

