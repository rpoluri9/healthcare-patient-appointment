package com.healthcare.appointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record PatientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Past LocalDate dateOfBirth,
        @Pattern(regexp = "^[+0-9][0-9 ()-]{6,}$", message = "Invalid phone number") String phone,
        @Email String email,
        String address) {
}
