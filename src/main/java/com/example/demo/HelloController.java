package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
//hello world sssssssssssssssss
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello World from Backend!";
    }

    @GetMapping("/time")
    public String getTime() {
        return "Current server time is: " + LocalDateTime.now();
    }

    @GetMapping("/greet")
    public String greetUser(@RequestParam(value = "name", defaultValue = "User") String name) {
        return "Hello, " + name + "!";
    }
    
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());
        status.put("service", "Spring Boot Demo");
        status.put("version", "1.0.0");
        return status;
    }
}