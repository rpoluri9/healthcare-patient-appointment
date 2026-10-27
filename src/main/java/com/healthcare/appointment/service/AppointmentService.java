package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.AppointmentRequest;
import com.healthcare.appointment.dto.AppointmentResponse;
import com.healthcare.appointment.exception.BookingConflictException;
import com.healthcare.appointment.exception.BookingValidationException;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.model.Appointment;
import com.healthcare.appointment.model.AppointmentStatus;
import com.healthcare.appointment.model.Patient;
import com.healthcare.appointment.model.Provider;
import com.healthcare.appointment.repository.AppointmentRepository;
import com.healthcare.appointment.repository.PatientRepository;
import com.healthcare.appointment.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Appointment scheduling. Rules, checked in order:
 * 1. Patient and provider must exist.
 * 2. The slot must be in the future and end after it starts.
 * 3. The slot length is derived from the provider's consultation duration,
 *    so a client can never book a slot of the wrong length.
 * 4. The day must be one of the provider's working days.
 * 5. The slot must fit inside the provider's working hours.
 * 6. No overlapping BOOKED appointment for the provider (409 on violation).
 * 7. No overlapping BOOKED appointment for the patient (409 on violation).
 * Logs carry IDs only — never patient or provider names.
 */
@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final ProviderRepository providers;

    public AppointmentService(AppointmentRepository appointments,
                              PatientRepository patients,
                              ProviderRepository providers) {
        this.appointments = appointments;
        this.patients = patients;
        this.providers = providers;
    }

    @Transactional
    public AppointmentResponse book(AppointmentRequest req) {
        Patient patient = patients.findById(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + req.patientId()));
        Provider provider = providers.findById(req.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + req.providerId()));

        LocalDateTime start = req.startTime();
        // End time is derived from the provider's consultation duration, so a
        // client can never book a slot of the wrong length (rule 3 by design).
        LocalDateTime end = start.plusMinutes(provider.getConsultationDurationMinutes());

        if (start.isBefore(LocalDateTime.now())) {
            throw new BookingValidationException("Appointments cannot be booked in the past.");
        }
        if (!provider.workingDaySet().contains(start.getDayOfWeek())) {
            throw new BookingValidationException("Provider does not work on " + start.getDayOfWeek() + ".");
        }
        if (start.toLocalTime().isBefore(provider.getWorkStartTime())
                || end.toLocalTime().isAfter(provider.getWorkEndTime())) {
            throw new BookingValidationException("Appointment must fit inside provider working hours "
                    + provider.getWorkStartTime() + "-" + provider.getWorkEndTime() + ".");
        }
        if (!appointments.findProviderOverlaps(provider.getId(), start, end).isEmpty()) {
            throw new BookingConflictException("Provider already has a booking overlapping this slot.");
        }
        if (!appointments.findPatientOverlaps(patient.getId(), start, end).isEmpty()) {
            throw new BookingConflictException("Patient already has a booking overlapping this slot.");
        }

        Appointment saved = appointments.save(
                new Appointment(patient, provider, start, end, req.reason()));
        log.info("Booked appointment id={} patientId={} providerId={}",
                saved.getId(), patient.getId(), provider.getId());
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancel(Long id) {
        Appointment a = getOrThrow(id);
        if (a.getStatus() != AppointmentStatus.BOOKED) {
            throw new BookingValidationException("Only a BOOKED appointment can be cancelled.");
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        log.info("Cancelled appointment id={}", id);
        return toResponse(a);
    }

    @Transactional
    public AppointmentResponse complete(Long id) {
        Appointment a = getOrThrow(id);
        if (a.getStatus() != AppointmentStatus.BOOKED) {
            throw new BookingValidationException("Only a BOOKED appointment can be completed.");
        }
        a.setStatus(AppointmentStatus.COMPLETED);
        log.info("Completed appointment id={}", id);
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> search(Long patientId, Long providerId,
                                            AppointmentStatus status,
                                            LocalDateTime from, LocalDateTime to) {
        List<Appointment> all;
        if (patientId != null && status != null) {
            all = appointments.findByPatientIdAndStatus(patientId, status);
        } else if (patientId != null) {
            all = appointments.findByPatientId(patientId);
        } else if (providerId != null) {
            all = appointments.findByProviderId(providerId);
        } else if (status != null) {
            all = appointments.findByStatus(status);
        } else if (from != null && to != null) {
            all = appointments.findByStartTimeBetween(from, to);
        } else {
            all = appointments.findAll();
        }
        return all.stream().map(AppointmentService::toResponse).toList();
    }

    private Appointment getOrThrow(Long id) {
        return appointments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    public static AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(),
                a.getPatient().getId(),
                a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getProvider().getId(),
                a.getProvider().getFirstName() + " " + a.getProvider().getLastName(),
                a.getStartTime(), a.getEndTime(), a.getStatus(), a.getReason());
    }
}
