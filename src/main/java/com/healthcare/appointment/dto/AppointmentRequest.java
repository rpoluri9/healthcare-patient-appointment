package com.healthcare.appointment.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull Long patientId,
        @NotNull Long providerId,
        @NotNull LocalDateTime startTime,
        String reason) {
}
