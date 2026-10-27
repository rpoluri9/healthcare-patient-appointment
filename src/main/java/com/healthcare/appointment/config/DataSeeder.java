package com.healthcare.appointment.config;

import com.healthcare.appointment.model.Patient;
import com.healthcare.appointment.model.Provider;
import com.healthcare.appointment.model.Role;
import com.healthcare.appointment.model.UserAccount;
import com.healthcare.appointment.repository.PatientRepository;
import com.healthcare.appointment.repository.ProviderRepository;
import com.healthcare.appointment.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Demo-only seed data. Every patient and provider here is FICTIONAL demo
 * content created for this portfolio project — no real people, no real
 * clinic. Demo passwords are documented in the README and must be changed
 * before any real use; this is a local learning setup, not a production
 * identity system (no external OAuth provider is involved anywhere).
 */
@Configuration
public class DataSeeder {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner seed(PatientRepository patients,
                                  ProviderRepository providers,
                                  UserAccountRepository users,
                                  PasswordEncoder encoder) {
        return args -> {
            Patient john = patients.save(new Patient("John", "Doe",
                    LocalDate.of(1990, 5, 14), "+1-555-0100",
                    "john.doe@example.com", "12 Demo Street"));
            patients.save(new Patient("Jane", "Smith",
                    LocalDate.of(1985, 9, 2), "+1-555-0101",
                    "jane.smith@example.com", "34 Demo Avenue"));
            providers.save(new Provider("Alice", "Garcia", "Cardiology",
                    "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
                    LocalTime.of(9, 0), LocalTime.of(17, 0), 30));
            providers.save(new Provider("Bob", "Lee", "Dermatology",
                    "MONDAY,WEDNESDAY,FRIDAY",
                    LocalTime.of(10, 0), LocalTime.of(16, 0), 45));

            users.save(new UserAccount("admin", encoder.encode("admin123"),
                    Role.ADMIN, null));
            users.save(new UserAccount("staff", encoder.encode("staff123"),
                    Role.STAFF, null));
            users.save(new UserAccount("patient1", encoder.encode("patient123"),
                    Role.PATIENT, john));
        };
    }
}
