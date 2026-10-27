package com.healthcare.appointment.dto;

import com.healthcare.appointment.model.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentResponse(Long id, Long patientId, String patientName,
                                  Long providerId, String providerName,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  AppointmentStatus status, String reason) {
}
