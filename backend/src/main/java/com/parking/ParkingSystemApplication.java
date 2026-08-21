package com.parking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Single entry point for the whole parking system: authentication, the
 * shared parking inventory, end-user bookings, the operator gate flow, and
 * the admin console. (Previously this class lived under com.parking.auth
 * and was named AuthServiceApplication, a holdover from when auth was the
 * only module — moved to the root package now that it boots everything.)
 */
@SpringBootApplication(scanBasePackages = {
        "com.parking.auth", "com.parking.inventory", "com.parking.booking",
        "com.parking.operator", "com.parking.admin", "com.parking.dashboard"
})
@EnableJpaRepositories(basePackages = {
        "com.parking.auth.repository", "com.parking.inventory.repository", "com.parking.booking.repository",
        "com.parking.operator.repository", "com.parking.admin.repository"
})
@EntityScan(basePackages = {
        "com.parking.auth.model", "com.parking.inventory.model", "com.parking.booking.model",
        "com.parking.operator.models", "com.parking.admin.model"
})
@EnableScheduling
public class ParkingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParkingSystemApplication.class, args);
    }
}
