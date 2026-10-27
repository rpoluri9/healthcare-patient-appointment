package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.UserInfoResponse;
import com.healthcare.appointment.model.UserAccount;
import com.healthcare.appointment.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUserService currentUser;

    public AuthController(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public UserInfoResponse me() {
        UserAccount me = currentUser.currentAccount();
        Long linkedId = me.getLinkedPatient() == null ? null : me.getLinkedPatient().getId();
        return new UserInfoResponse(me.getUsername(), me.getRole(), linkedId);
    }
}
