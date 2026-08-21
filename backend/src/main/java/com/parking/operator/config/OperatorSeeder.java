package com.parking.operator.config;

import com.parking.operator.models.Operator;
import com.parking.operator.repository.OperatorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo gate-staff accounts. Slot/lot inventory is seeded once, for
 * every module, by {@link com.parking.inventory.config.ParkingInventorySeeder}.
 */
@Component
@Order(2)
public class OperatorSeeder implements CommandLineRunner {

    private final OperatorRepository operatorRepo;
    private final PasswordEncoder    passwordEncoder;

    public OperatorSeeder(OperatorRepository operatorRepo, PasswordEncoder passwordEncoder) {
        this.operatorRepo    = operatorRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Idempotent: ensure the demo operators exist (and reset credentials)
        // so local/dev databases don't get "stuck" with unknown passwords.
        seedOrUpdateOp("OP001", "12345678",     "Nagadivya",   "Gate 1", Operator.OperatorRole.ENTRY_OPERATOR);
        seedOrUpdateOp("OP002", "operator123",  "Jane Doe",    "Gate 2", Operator.OperatorRole.EXIT_OPERATOR);
        seedOrUpdateOp("OP003", "supervisor1",  "Mike Johnson","Gate 1", Operator.OperatorRole.SUPERVISOR);
    }

    private void seedOrUpdateOp(String id, String pass, String name,
                                String gate, Operator.OperatorRole role) {
        Operator op = operatorRepo.findByOperatorId(id).orElseGet(Operator::new);
        op.setOperatorId(id);
        op.setPassword(passwordEncoder.encode(pass));
        op.setFullName(name);
        op.setGateName(gate);
        op.setLotName("City Center Parking");
        op.setRole(role);
        op.setActive(true);
        operatorRepo.save(op);
    }
}
