package com.example.smartBiz.config;

import com.example.smartBiz.entity.AppUser;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.enums.Role;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.repository.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    private final BusinessRepo businessRepo;

    public AdminSeeder(BusinessRepo businessRepo) {
        this.businessRepo = businessRepo;
    }

    @Bean
    public CommandLineRunner seedAdmin(UserRepo userRepo, PasswordEncoder encoder) {
        return args -> {

            // ✅ If admin already exists, do nothing
            if (userRepo.existsByEmail("admin123@gmail.com")) {
                return;
            }

            // ✅ Create or reuse SYSTEM business
            Business systemBusiness = businessRepo.findById(1L)
                    .orElseGet(() -> {
                        Business b = new Business();
                        b.setName("SYSTEM");
                        return businessRepo.save(b);
                    });

            // ✅ Create ADMIN user
            AppUser admin = new AppUser();
            admin.setEmail("admin123@gmail.com");
            admin.setPassword(encoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setBusiness(systemBusiness); // NOT NULL ✔

            userRepo.save(admin);

            System.out.println("✅ SYSTEM ADMIN CREATED: admin@smartbiz.com / admin123");
        };
    }
}
