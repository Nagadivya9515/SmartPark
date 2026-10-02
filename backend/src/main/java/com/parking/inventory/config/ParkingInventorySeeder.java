package com.parking.inventory.config;

import com.parking.inventory.model.ParkingLot;
import com.parking.inventory.model.ParkingSlot;
import com.parking.inventory.model.VehicleType;
import com.parking.inventory.repository.ParkingLotRepository;
import com.parking.inventory.repository.ParkingSlotRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the single lot + slot inventory shared by the dashboard, operator,
 * and booking modules. Replaces the old dashboard.DataSeeder ("GreenView
 * Mall Parking") and operator.OperatorSeeder's slot half ("City Center
 * Parking") — those seeded two different, disconnected inventories; this
 * seeds one.
 */
@Component
@Order(1)
public class ParkingInventorySeeder implements CommandLineRunner {

    private final ParkingLotRepository lotRepo;
    private final ParkingSlotRepository slotRepo;

    public ParkingInventorySeeder(ParkingLotRepository lotRepo, ParkingSlotRepository slotRepo) {
        this.lotRepo = lotRepo;
        this.slotRepo = slotRepo;
    }

    @Override
    public void run(String... args) {
        if (lotRepo.count() > 0) return; // already seeded

        ParkingLot lot = new ParkingLot();
        lot.setName("City Center Parking");
        lot.setAddress("123 Main Street, Downtown");
        lot.setRating(4.5);
        lot.setTotalFloors(2);
        lot = lotRepo.save(lot);

        seedSection(lot, "Floor 1", "A", VehicleType.CAR,   20, 12, 5.0);
        seedSection(lot, "Floor 1", "B", VehicleType.BIKE,  15,  8, 1.5);
        seedSection(lot, "Floor 2", "C", VehicleType.CAR,   20, 10, 5.0);
        seedSection(lot, "Floor 2", "D", VehicleType.TRUCK, 15, 10, 8.0);
    }

    private void seedSection(ParkingLot lot, String floor, String section,
                              VehicleType type, int total, int occupied, double rate) {
        for (int i = 1; i <= total; i++) {
            ParkingSlot s = new ParkingSlot();
            s.setLot(lot);
            s.setSlotNumber(String.format("%02d", i));
            s.setDisplayNumber(section + "-" + String.format("%02d", i));
            s.setSectionName(section);
            s.setFloorName(floor);
            s.setVehicleType(type);
            s.setOccupied(i <= occupied);
            s.setRatePerHour(rate);
            slotRepo.save(s);
        }
    }
}
