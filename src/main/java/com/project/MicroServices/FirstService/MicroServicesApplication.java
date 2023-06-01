package com.project.MicroServices.FirstService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collection;
import java.util.Collections;

@SpringBootApplication
public class MicroServicesApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(MicroServicesApplication.class);
        springApplication.setDefaultProperties(Collections.singletonMap("server.port","8082"));
        springApplication.run(args);

    }
}




