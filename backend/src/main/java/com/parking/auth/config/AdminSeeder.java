package com.parking.auth.config;

import com.parking.admin.model.AdminUser;
import com.parking.admin.repository.AdminUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminSeeder implements CommandLineRunner {

    private final AdminUserRepository adminRepo;
    private final PasswordEncoder     passwordEncoder;

    public AdminSeeder(AdminUserRepository adminRepo, PasswordEncoder passwordEncoder) {
        this.adminRepo       = adminRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminRepo.count() > 0) return;

        // Super admin
        AdminUser superAdmin = new AdminUser();
        superAdmin.setUsername("admin@smartpark.com");
        superAdmin.setPassword(passwordEncoder.encode("Admin@1234"));
        superAdmin.setFullName("System Admin");
        superAdmin.setRole(AdminUser.AdminRole.SUPER_ADMIN);
        superAdmin.setActive(true);
        adminRepo.save(superAdmin);

        // Regular admin
        AdminUser admin = new AdminUser();
        admin.setUsername("manager@smartpark.com");
        admin.setPassword(passwordEncoder.encode("Manager@1234"));
        admin.setFullName("Parking Manager");
        admin.setRole(AdminUser.AdminRole.ADMIN);
        admin.setActive(true);
        adminRepo.save(admin);
    }
}