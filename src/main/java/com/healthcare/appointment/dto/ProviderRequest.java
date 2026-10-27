package com.healthcare.appointment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record ProviderRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String specialization,
        @NotBlank String workingDays,
        @NotNull LocalTime workStartTime,
        @NotNull LocalTime workEndTime,
        @Min(5) int consultationDurationMinutes) {
}
