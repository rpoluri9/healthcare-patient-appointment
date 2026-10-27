package com.healthcare.appointment.dto;

import java.time.LocalDate;

/** Full patient record — only ever returned for one patient at a time, to an authorized caller. */
public record PatientDetailResponse(Long id, String firstName, String lastName,
                                    LocalDate dateOfBirth, String phone,
                                    String email, String address) {
}
