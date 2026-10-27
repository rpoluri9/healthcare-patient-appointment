package com.healthcare.appointment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end flow tests with REAL authentication (HTTP Basic against the
 * seeded demo users), proving the role and ownership rules behave as documented.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerTest {

    @Autowired
    private MockMvc mvc;

    private static String nextMonday10am() {
        LocalDateTime slot = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10, 0);
        return slot.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String nextSaturday10am() {
        LocalDateTime slot = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).atTime(10, 0);
        return slot.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private Long createPatient() throws Exception {
        MvcResult res = mvc.perform(post("/api/patients")
                        .with(httpBasic("staff", "staff123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Flow","lastName":"Patient",
                                "dateOfBirth":"1991-04-04","phone":"+1-555-0300",
                                "email":"flow@example.com","address":"9 Demo Ln"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String body = res.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private Long createProvider() throws Exception {
        MvcResult res = mvc.perform(post("/api/providers")
                        .with(httpBasic("staff", "staff123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Flow","lastName":"Doctor",
                                "specialization":"General",
                                "workingDays":"MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
                                "workStartTime":"09:00:00","workEndTime":"17:00:00",
                                "consultationDurationMinutes":30}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String body = res.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
    }

    @Test
    void patientRoleCannotCreatePatientsOrProviders() throws Exception {
        mvc.perform(post("/api/patients")
                        .with(httpBasic("patient1", "patient123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"No","lastName":"Perm",
                                "dateOfBirth":"1990-01-01","phone":"+1-555-0301",
                                "email":"no@example.com","address":"x"}"""))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/providers")
                        .with(httpBasic("patient1", "patient123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"No","lastName":"Perm",
                                "specialization":"General","workingDays":"MONDAY",
                                "workStartTime":"09:00:00","workEndTime":"17:00:00",
                                "consultationDurationMinutes":30}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullBookingFlowWithConflictAndCancel() throws Exception {
        Long patientId = createPatient();
        Long providerId = createProvider();
        String slot = nextMonday10am();
        String booking = """
                {"patientId":%d,"providerId":%d,"startTime":"%s","reason":"Checkup"}"""
                .formatted(patientId, providerId, slot);

        MvcResult booked = mvc.perform(post("/api/appointments")
                        .with(httpBasic("staff", "staff123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn();
        Long appointmentId = Long.parseLong(
                booked.getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1"));

        // Same slot again -> 409 conflict.
        mvc.perform(post("/api/appointments")
                        .with(httpBasic("staff", "staff123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking))
                .andExpect(status().isConflict());

        // Saturday booking -> 400 (provider does not work weekends).
        String saturdayBooking = """
                {"patientId":%d,"providerId":%d,"startTime":"%s","reason":"Weekend"}"""
                .formatted(patientId, providerId, nextSaturday10am());
        mvc.perform(post("/api/appointments")
                        .with(httpBasic("staff", "staff123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saturdayBooking))
                .andExpect(status().isBadRequest());

        // Search by patient finds the booking.
        mvc.perform(get("/api/appointments")
                        .with(httpBasic("staff", "staff123"))
                        .param("patientId", patientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Cancel, then the slot frees up and the search shows CANCELLED.
        mvc.perform(post("/api/appointments/" + appointmentId + "/cancel")
                        .with(httpBasic("staff", "staff123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/appointments")
                        .with(httpBasic("staff", "staff123"))
                        .param("patientId", patientId.toString())
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void patientCanOnlyTouchOwnRecords() throws Exception {
        // patient1's linked patient id, from /api/auth/me.
        MvcResult me = mvc.perform(get("/api/auth/me")
                        .with(httpBasic("patient1", "patient123")))
                .andExpect(status().isOk())
                .andReturn();
        Long ownId = Long.parseLong(
                me.getResponse().getContentAsString().replaceAll(".*\"linkedPatientId\":(\\d+).*", "$1"));

        // Own record: allowed; another patient: forbidden.
        mvc.perform(get("/api/patients/" + ownId)
                        .with(httpBasic("patient1", "patient123")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/patients/" + (ownId + 1))
                        .with(httpBasic("patient1", "patient123")))
                .andExpect(status().isForbidden());
        // Patient list is staff/admin only.
        mvc.perform(get("/api/patients")
                        .with(httpBasic("patient1", "patient123")))
                .andExpect(status().isForbidden());
        // Booking for someone else is forbidden; booking for self works.
        Long providerId = createProvider();
        String otherBooking = """
                {"patientId":%d,"providerId":%d,"startTime":"%s","reason":"Not mine"}"""
                .formatted(ownId + 1, providerId, nextMonday10am());
        mvc.perform(post("/api/appointments")
                        .with(httpBasic("patient1", "patient123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otherBooking))
                .andExpect(status().isForbidden());
        String ownBooking = """
                {"patientId":%d,"providerId":%d,"startTime":"%s","reason":"My visit"}"""
                .formatted(ownId, providerId, nextMonday10am());
        mvc.perform(post("/api/appointments")
                        .with(httpBasic("patient1", "patient123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownBooking))
                .andExpect(status().isCreated());
        // Completing is staff/admin only.
        mvc.perform(post("/api/appointments/1/complete")
                        .with(httpBasic("patient1", "patient123")))
                .andExpect(status().isForbidden());
    }
}
