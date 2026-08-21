package com.parking.booking.config;

import com.parking.inventory.model.VehicleType;
import com.parking.booking.model.UserVehicle;
import com.parking.booking.repository.UserVehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds demo vehicles for user 1. (No longer seeds a demo completed booking —
 * a seeded Booking now needs a real ParkingSlot foreign key, and inventing
 * one at seed time added complexity for no real benefit; "my bookings"
 * simply starts empty until the demo user books something for real.)
 */
@Component
@Order(3)
public class BookingSeeder implements CommandLineRunner {

    private final UserVehicleRepository vehicleRepo;

    public BookingSeeder(UserVehicleRepository vehicleRepo) {
        this.vehicleRepo = vehicleRepo;
    }

    @Override
    public void run(String... args) {
        if (vehicleRepo.count() > 0) return;

        UserVehicle v1 = new UserVehicle();
        v1.setUserId(1L); v1.setVehicleNumber("ABC-1234");
        v1.setVehicleLabel("Toyota Camry"); v1.setVehicleType(VehicleType.CAR);
        vehicleRepo.save(v1);

        UserVehicle v2 = new UserVehicle();
        v2.setUserId(1L); v2.setVehicleNumber("XYZ-5678");
        v2.setVehicleLabel("Honda CBR"); v2.setVehicleType(VehicleType.BIKE);
        vehicleRepo.save(v2);
    }
}
