package com.example.physiciansservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PhysiciansServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhysiciansServiceApplication.class, args);
    }

}
