package com.example.tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiTests {

    static Playwright playwright;
    static APIRequestContext request;

    @BeforeAll
    static void setup() {
        playwright = Playwright.create();
        request = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL("http://192.168.1.58:8081")); // change this back to "http://192.168.1.2:8081" when at home
    }

    @AfterAll
    static void teardown() {
        request.dispose();
        playwright.close();
    }

    @Test
    void testHelloEndpoint() {
        APIResponse response = request.get("/hello");
        assertEquals(200, response.status());
        assertEquals("Hello World from Backend!", response.text());
    }

    @Test
    void testTimeEndpoint() {
        APIResponse response = request.get("/time");
        assertEquals(200, response.status());
        assertTrue(response.text().contains("Current server time is:"));
    }

    @Test
    void testGreetEndpoint() {
        APIResponse response = request.get("/greet?name=Hatem");
        assertEquals(200, response.status());
        assertEquals("Hello, Hatem!", response.text());
    }

    @Test
    void testGreetEndpointDefaultValue() {
        APIResponse response = request.get("/greet");
        assertEquals(200, response.status());
        assertEquals("Hello, User!", response.text()); // Default name should be "User"
    }
}
