package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.AppointmentRequest;
import com.healthcare.appointment.exception.BookingConflictException;
import com.healthcare.appointment.exception.BookingValidationException;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.model.Appointment;
import com.healthcare.appointment.model.AppointmentStatus;
import com.healthcare.appointment.model.Patient;
import com.healthcare.appointment.model.Provider;
import com.healthcare.appointment.repository.AppointmentRepository;
import com.healthcare.appointment.repository.PatientRepository;
import com.healthcare.appointment.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointments;
    @Mock
    private PatientRepository patients;
    @Mock
    private ProviderRepository providers;

    private AppointmentService service;
    private Patient patient;
    private Provider provider;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointments, patients, providers);
        patient = new Patient("Test", "Patient", LocalDate.of(1990, 1, 1),
                "+1-555-0000", "test@example.com", "1 Test St");
        provider = new Provider("Test", "Doctor", "Cardiology",
                "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
                LocalTime.of(9, 0), LocalTime.of(17, 0), 30);
        setField(patient, "id", 1L);
        setField(provider, "id", 1L);
        when(patients.findById(1L)).thenReturn(Optional.of(patient));
        when(providers.findById(1L)).thenReturn(Optional.of(provider));
        when(appointments.save(any(Appointment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /** Next Monday at 10:00 — always a future weekday inside working hours. */
    private LocalDateTime nextMonday10am() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10, 0);
    }

    @Test
    void booksAppointmentWithDerivedEndTime() {
        LocalDateTime start = nextMonday10am();
        when(appointments.findProviderOverlaps(1L, start, start.plusMinutes(30)))
                .thenReturn(List.of());
        when(appointments.findPatientOverlaps(1L, start, start.plusMinutes(30)))
                .thenReturn(List.of());

        var res = service.book(new AppointmentRequest(1L, 1L, start, "Checkup"));

        assertEquals(AppointmentStatus.BOOKED, res.status());
        assertEquals(start.plusMinutes(30), res.endTime());
    }

    @Test
    void rejectsOverlappingProviderBooking() {
        LocalDateTime start = nextMonday10am();
        Appointment existing = new Appointment(patient, provider, start,
                start.plusMinutes(30), "Existing");
        when(appointments.findProviderOverlaps(1L, start, start.plusMinutes(30)))
                .thenReturn(List.of(existing));

        assertThrows(BookingConflictException.class,
                () -> service.book(new AppointmentRequest(1L, 1L, start, "Clash")));
    }

    @Test
    void rejectsOverlappingPatientBooking() {
        LocalDateTime start = nextMonday10am();
        Appointment existing = new Appointment(patient, provider, start,
                start.plusMinutes(30), "Existing");
        when(appointments.findProviderOverlaps(1L, start, start.plusMinutes(30)))
                .thenReturn(List.of());
        when(appointments.findPatientOverlaps(1L, start, start.plusMinutes(30)))
                .thenReturn(List.of(existing));

        assertThrows(BookingConflictException.class,
                () -> service.book(new AppointmentRequest(1L, 1L, start, "Clash")));
    }

    @Test
    void rejectsBookingOutsideWorkingHours() {
        LocalDateTime start = nextMonday10am().withHour(18).withMinute(0);
        assertThrows(BookingValidationException.class,
                () -> service.book(new AppointmentRequest(1L, 1L, start, "Late")));
    }

    @Test
    void rejectsBookingOnNonWorkingDay() {
        LocalDateTime saturday = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).atTime(10, 0);
        assertThrows(BookingValidationException.class,
                () -> service.book(new AppointmentRequest(1L, 1L, saturday, "Weekend")));
    }

    @Test
    void rejectsBookingInThePast() {
        LocalDateTime past = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0);
        assertThrows(BookingValidationException.class,
                () -> service.book(new AppointmentRequest(1L, 1L, past, "Past")));
    }

    @Test
    void rejectsUnknownPatientAndProvider() {
        LocalDateTime start = nextMonday10am();
        assertThrows(ResourceNotFoundException.class,
                () -> service.book(new AppointmentRequest(99L, 1L, start, "No patient")));
        assertThrows(ResourceNotFoundException.class,
                () -> service.book(new AppointmentRequest(1L, 99L, start, "No provider")));
    }

    @Test
    void cancelAndCompleteChangeStatus() {
        Appointment booked = new Appointment(patient, provider,
                nextMonday10am(), nextMonday10am().plusMinutes(30), "Visit");
        when(appointments.findById(10L)).thenReturn(Optional.of(booked));

        assertEquals(AppointmentStatus.CANCELLED, service.cancel(10L).status());
        assertThrows(BookingValidationException.class, () -> service.cancel(10L));

        Appointment booked2 = new Appointment(patient, provider,
                nextMonday10am(), nextMonday10am().plusMinutes(30), "Visit");
        when(appointments.findById(11L)).thenReturn(Optional.of(booked2));
        assertEquals(AppointmentStatus.COMPLETED, service.complete(11L).status());
    }
}
