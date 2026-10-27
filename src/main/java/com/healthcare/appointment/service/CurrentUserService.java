package com.healthcare.appointment.service;

import com.healthcare.appointment.exception.ForbiddenException;
import com.healthcare.appointment.model.Role;
import com.healthcare.appointment.model.UserAccount;
import com.healthcare.appointment.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolves the authenticated caller and enforces ownership: a PATIENT caller
 * may only touch the patient record linked to their own account. STAFF and
 * ADMIN are trusted for all records. Throws ForbiddenException otherwise.
 */
@Service
public class CurrentUserService {

    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    public UserAccount currentAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ForbiddenException("No authenticated user.");
        }
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Unknown user: " + auth.getName()));
    }

    public boolean isStaffOrAdmin() {
        Role role = currentAccount().getRole();
        return role == Role.STAFF || role == Role.ADMIN;
    }

    /** PATIENT callers must pass their own linked patient id; staff/admin pass anything. */
    public void requireAccessToPatient(Long patientId) {
        UserAccount me = currentAccount();
        if (me.getRole() == Role.PATIENT) {
            if (me.getLinkedPatient() == null
                    || !me.getLinkedPatient().getId().equals(patientId)) {
                throw new ForbiddenException("Patients may only access their own records.");
            }
        }
    }
}
