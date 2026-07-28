package com.unihub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // Endpoint này hứng request từ Nginx
    @GetMapping("/api/test")
    public String testEndpoint(@RequestHeader(value = "X-User-Id", defaultValue = "GUEST") String userId) {
        // In log ra console để bạn xem Spring Boot có nhận được đạn không
        System.out.println(">>> Received request from User ID: " + userId);
        
        return String.format(
            "{\"status\": 200, \"message\": \"Hello from Spring Boot!\", \"user\": \"%s\"}", 
            userId
        );
    }
}