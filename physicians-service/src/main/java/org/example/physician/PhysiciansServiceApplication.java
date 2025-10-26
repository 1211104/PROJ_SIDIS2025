package org.example.physician;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.example.physician")
public class PhysiciansServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhysiciansServiceApplication.class, args);
    }

}

