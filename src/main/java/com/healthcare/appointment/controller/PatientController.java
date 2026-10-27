package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.PatientDetailResponse;
import com.healthcare.appointment.dto.PatientRequest;
import com.healthcare.appointment.dto.PatientSummaryResponse;
import com.healthcare.appointment.exception.ForbiddenException;
import com.healthcare.appointment.service.CurrentUserService;
import com.healthcare.appointment.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patients;
    private final CurrentUserService currentUser;

    public PatientController(PatientService patients, CurrentUserService currentUser) {
        this.patients = patients;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDetailResponse register(@Valid @RequestBody PatientRequest req) {
        return patients.register(req);
    }

    @PutMapping("/{id}")
    public PatientDetailResponse update(@PathVariable Long id,
                                        @Valid @RequestBody PatientRequest req) {
        return patients.update(id, req);
    }

    /** List view only returns summaries — no sensitive fields leave the API here. */
    @GetMapping
    public List<PatientSummaryResponse> list() {
        if (!currentUser.isStaffOrAdmin()) {
            throw new ForbiddenException("Only staff or admin may list patients.");
        }
        return patients.list();
    }

    /** Full detail: staff/admin, or the patient themselves. */
    @GetMapping("/{id}")
    public PatientDetailResponse get(@PathVariable Long id) {
        currentUser.requireAccessToPatient(id);
        return patients.getDetail(id);
    }
}
