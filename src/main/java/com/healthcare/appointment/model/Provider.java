package com.healthcare.appointment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A healthcare provider. Working days are stored as a comma-separated list of
 * DayOfWeek names (e.g. "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY") so the
 * scheduling rules can check provider availability without extra tables.
 */
@Entity
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String specialization;

    @NotBlank
    private String workingDays;

    @NotNull
    private LocalTime workStartTime;

    @NotNull
    private LocalTime workEndTime;

    @Min(5)
    private int consultationDurationMinutes;

    protected Provider() {
    }

    public Provider(String firstName, String lastName, String specialization,
                    String workingDays, LocalTime workStartTime,
                    LocalTime workEndTime, int consultationDurationMinutes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.specialization = specialization;
        this.workingDays = workingDays;
        this.workStartTime = workStartTime;
        this.workEndTime = workEndTime;
        this.consultationDurationMinutes = consultationDurationMinutes;
    }

    /** Parses {@link #workingDays} into a set of {@link DayOfWeek}. */
    public Set<DayOfWeek> workingDaySet() {
        return Arrays.stream(workingDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> DayOfWeek.valueOf(s.toUpperCase()))
                .collect(Collectors.toSet());
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getWorkingDays() { return workingDays; }
    public void setWorkingDays(String workingDays) { this.workingDays = workingDays; }
    public LocalTime getWorkStartTime() { return workStartTime; }
    public void setWorkStartTime(LocalTime workStartTime) { this.workStartTime = workStartTime; }
    public LocalTime getWorkEndTime() { return workEndTime; }
    public void setWorkEndTime(LocalTime workEndTime) { this.workEndTime = workEndTime; }
    public int getConsultationDurationMinutes() { return consultationDurationMinutes; }
    public void setConsultationDurationMinutes(int minutes) { this.consultationDurationMinutes = minutes; }
}
