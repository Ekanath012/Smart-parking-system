package com.example.parking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application Class
 * Run this class to start the Smart Parking Management System
 */
@SpringBootApplication
@EnableScheduling
public class SmartParkingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartParkingApplication.class, args);
        System.out.println("==========================================");
        System.out.println("  Smart Parking System is Running!");
        System.out.println("  Open: http://localhost:8080");
        System.out.println("==========================================");
    }
}
