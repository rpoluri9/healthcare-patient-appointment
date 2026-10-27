package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.PatientDetailResponse;
import com.healthcare.appointment.dto.PatientRequest;
import com.healthcare.appointment.dto.PatientSummaryResponse;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.model.Patient;
import com.healthcare.appointment.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Patient management. Logging rule: only patient IDs are ever logged — never
 * names, dates of birth, phone numbers, email addresses, or home addresses.
 */
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patients;

    public PatientService(PatientRepository patients) {
        this.patients = patients;
    }

    @Transactional
    public PatientDetailResponse register(PatientRequest req) {
        Patient saved = patients.save(new Patient(req.firstName(), req.lastName(),
                req.dateOfBirth(), req.phone(), req.email(), req.address()));
        log.info("Registered patient id={}", saved.getId());
        return toDetail(saved);
    }

    @Transactional
    public PatientDetailResponse update(Long id, PatientRequest req) {
        Patient p = patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
        p.setFirstName(req.firstName());
        p.setLastName(req.lastName());
        p.setDateOfBirth(req.dateOfBirth());
        p.setPhone(req.phone());
        p.setEmail(req.email());
        p.setAddress(req.address());
        log.info("Updated patient id={}", id);
        return toDetail(p);
    }

    @Transactional(readOnly = true)
    public PatientDetailResponse getDetail(Long id) {
        return toDetail(patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<PatientSummaryResponse> list() {
        return patients.findAll().stream()
                .map(p -> new PatientSummaryResponse(p.getId(), p.getFirstName(), p.getLastName()))
                .toList();
    }

    public static PatientDetailResponse toDetail(Patient p) {
        return new PatientDetailResponse(p.getId(), p.getFirstName(), p.getLastName(),
                p.getDateOfBirth(), p.getPhone(), p.getEmail(), p.getAddress());
    }
}
