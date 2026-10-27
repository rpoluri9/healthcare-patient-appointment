package com.healthcare.appointment.dto;

import java.time.LocalTime;

public record ProviderResponse(Long id, String firstName, String lastName,
                               String specialization, String workingDays,
                               LocalTime workStartTime, LocalTime workEndTime,
                               int consultationDurationMinutes) {
}
