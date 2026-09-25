package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String sayHello(){
        return "Hello World from Spring Boot";
    }

    @GetMapping("/hello-json")
    public Map<String, Object> sayHelloJson(@RequestParam(defaultValue = "World") String name){
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, " + name + "!");
        response.put("status", 200);
        return response;
    }
}
