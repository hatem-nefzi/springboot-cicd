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
}
