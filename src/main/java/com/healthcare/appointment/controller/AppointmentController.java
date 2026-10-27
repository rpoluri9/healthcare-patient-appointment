package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.AppointmentRequest;
import com.healthcare.appointment.dto.AppointmentResponse;
import com.healthcare.appointment.exception.ForbiddenException;
import com.healthcare.appointment.model.AppointmentStatus;
import com.healthcare.appointment.model.Role;
import com.healthcare.appointment.model.UserAccount;
import com.healthcare.appointment.service.AppointmentService;
import com.healthcare.appointment.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointments;
    private final CurrentUserService currentUser;

    public AppointmentController(AppointmentService appointments,
                                 CurrentUserService currentUser) {
        this.appointments = appointments;
        this.currentUser = currentUser;
    }

    /**
     * Book an appointment. A PATIENT caller may only book for themselves:
     * a different patientId in the request is rejected with 403.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@Valid @RequestBody AppointmentRequest req) {
        UserAccount me = currentUser.currentAccount();
        if (me.getRole() == Role.PATIENT) {
            if (me.getLinkedPatient() == null
                    || !me.getLinkedPatient().getId().equals(req.patientId())) {
                throw new ForbiddenException("Patients may only book appointments for themselves.");
            }
        }
        return appointments.book(req);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        AppointmentResponse found = appointments.get(id);
        currentUser.requireAccessToPatient(found.patientId());
        return found;
    }

    /**
     * Search by patient, provider, status, and/or date range. A PATIENT caller
     * only ever sees their own appointments: another patientId is rejected,
     * and no patientId at all is scoped to their linked record.
     */
    @GetMapping
    public List<AppointmentResponse> search(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        UserAccount me = currentUser.currentAccount();
        if (me.getRole() == Role.PATIENT) {
            Long ownId = me.getLinkedPatient() == null ? null : me.getLinkedPatient().getId();
            if (patientId != null && !patientId.equals(ownId)) {
                throw new ForbiddenException("Patients may only search their own appointments.");
            }
            patientId = ownId;
        }
        return appointments.search(patientId, providerId, status, from, to);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id) {
        AppointmentResponse found = appointments.get(id);
        currentUser.requireAccessToPatient(found.patientId());
        return appointments.cancel(id);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id) {
        return appointments.complete(id);
    }
}
