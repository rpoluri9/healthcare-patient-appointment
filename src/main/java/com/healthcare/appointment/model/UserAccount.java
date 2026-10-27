package com.healthcare.appointment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

/**
 * A login account. Passwords are stored only as BCrypt hashes, never in plain
 * text. A PATIENT account may be linked to a Patient record so ownership
 * checks can prove "this is my own record" without trusting a client-sent id.
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient linkedPatient;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash, Role role,
                       Patient linkedPatient) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.linkedPatient = linkedPatient;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Patient getLinkedPatient() { return linkedPatient; }
}
