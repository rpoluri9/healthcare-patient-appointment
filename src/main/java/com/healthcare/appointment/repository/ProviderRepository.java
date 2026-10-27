package com.healthcare.appointment.repository;

import com.healthcare.appointment.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProviderRepository extends JpaRepository<Provider, Long> {
    List<Provider> findBySpecializationIgnoreCase(String specialization);
}
