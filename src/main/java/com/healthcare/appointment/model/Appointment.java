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
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private String reason;

    protected Appointment() {
    }

    public Appointment(Patient patient, Provider provider,
                       LocalDateTime startTime, LocalDateTime endTime,
                       String reason) {
        this.patient = patient;
        this.provider = provider;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AppointmentStatus.BOOKED;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Provider getProvider() { return provider; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public String getReason() { return reason; }
}
