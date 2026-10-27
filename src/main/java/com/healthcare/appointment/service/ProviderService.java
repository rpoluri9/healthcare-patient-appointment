package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.ProviderRequest;
import com.healthcare.appointment.dto.ProviderResponse;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.model.Provider;
import com.healthcare.appointment.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProviderService {

    private static final Logger log = LoggerFactory.getLogger(ProviderService.class);

    private final ProviderRepository providers;

    public ProviderService(ProviderRepository providers) {
        this.providers = providers;
    }

    @Transactional
    public ProviderResponse create(ProviderRequest req) {
        Provider saved = providers.save(new Provider(req.firstName(), req.lastName(),
                req.specialization(), req.workingDays(), req.workStartTime(),
                req.workEndTime(), req.consultationDurationMinutes()));
        log.info("Created provider id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ProviderResponse update(Long id, ProviderRequest req) {
        Provider p = providers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));
        p.setFirstName(req.firstName());
        p.setLastName(req.lastName());
        p.setSpecialization(req.specialization());
        p.setWorkingDays(req.workingDays());
        p.setWorkStartTime(req.workStartTime());
        p.setWorkEndTime(req.workEndTime());
        p.setConsultationDurationMinutes(req.consultationDurationMinutes());
        log.info("Updated provider id={}", id);
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public ProviderResponse get(Long id) {
        return toResponse(providers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> list(String specialization) {
        List<Provider> all = specialization == null || specialization.isBlank()
                ? providers.findAll()
                : providers.findBySpecializationIgnoreCase(specialization);
        return all.stream().map(ProviderService::toResponse).toList();
    }

    public static ProviderResponse toResponse(Provider p) {
        return new ProviderResponse(p.getId(), p.getFirstName(), p.getLastName(),
                p.getSpecialization(), p.getWorkingDays(), p.getWorkStartTime(),
                p.getWorkEndTime(), p.getConsultationDurationMinutes());
    }
}
