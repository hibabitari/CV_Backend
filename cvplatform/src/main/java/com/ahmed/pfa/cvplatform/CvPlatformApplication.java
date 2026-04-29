package com.ahmed.pfa.cvplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;  // ← ADD THIS

@SpringBootApplication
@EnableScheduling  // ← ADD THIS - Active scheduled tasks
public class CvPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CvPlatformApplication.class, args);
    }

}