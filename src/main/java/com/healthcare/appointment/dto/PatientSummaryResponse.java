package com.healthcare.appointment.dto;

/**
 * List-view shape for a patient: deliberately contains NO date of birth,
 * phone, email, or address. Full details are only returned by the single
 * patient endpoint, and only to STAFF/ADMIN or the patient themselves.
 */
public record PatientSummaryResponse(Long id, String firstName, String lastName) {
}
