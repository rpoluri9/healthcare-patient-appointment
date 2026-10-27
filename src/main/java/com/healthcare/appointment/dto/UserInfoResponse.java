package com.healthcare.appointment.dto;

import com.healthcare.appointment.model.Role;

/** Who the caller is — never includes a password hash. */
public record UserInfoResponse(String username, Role role, Long linkedPatientId) {
}
