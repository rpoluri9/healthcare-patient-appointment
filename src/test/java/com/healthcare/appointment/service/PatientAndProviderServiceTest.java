package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.PatientRequest;
import com.healthcare.appointment.dto.ProviderRequest;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.model.Patient;
import com.healthcare.appointment.model.Provider;
import com.healthcare.appointment.repository.PatientRepository;
import com.healthcare.appointment.repository.ProviderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAndProviderServiceTest {

    @Mock
    private PatientRepository patients;
    @Mock
    private ProviderRepository providers;

    @Test
    void registersAndUpdatesPatient() {
        PatientService service = new PatientService(patients);
        PatientRequest req = new PatientRequest("Ann", "Ray",
                LocalDate.of(1992, 3, 3), "+1-555-0200",
                "ann@example.com", "5 Demo Rd");
        when(patients.save(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            return p;
        });
        var created = service.register(req);
        assertEquals("Ann", created.firstName());
        assertEquals("ann@example.com", created.email());

        Patient existing = new Patient("Ann", "Ray", LocalDate.of(1992, 3, 3),
                "+1-555-0200", "ann@example.com", "5 Demo Rd");
        when(patients.findById(7L)).thenReturn(Optional.of(existing));
        var updated = service.update(7L, new PatientRequest("Ann", "Ray",
                LocalDate.of(1992, 3, 3), "+1-555-0201",
                "ann@example.com", "6 Demo Rd"));
        assertEquals("+1-555-0201", updated.phone());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, req));
    }

    @Test
    void listsPatientSummariesWithoutSensitiveFields() {
        PatientService service = new PatientService(patients);
        when(patients.findAll()).thenReturn(List.of(
                new Patient("Ann", "Ray", LocalDate.of(1992, 3, 3),
                        "+1-555-0200", "ann@example.com", "5 Demo Rd")));
        var summaries = service.list();
        assertEquals(1, summaries.size());
        assertEquals("Ann", summaries.get(0).firstName());
        // PatientSummaryResponse has no dateOfBirth/phone/email/address fields
        // by construction — the compiler enforces the minimization.
    }

    @Test
    void createsProviderAndFiltersBySpecialization() {
        ProviderService service = new ProviderService(providers);
        ProviderRequest req = new ProviderRequest("Cid", "Park", "Neurology",
                "MONDAY,TUESDAY", LocalTime.of(9, 0), LocalTime.of(15, 0), 60);
        when(providers.save(any(Provider.class))).thenAnswer(inv -> inv.getArgument(0));
        var created = service.create(req);
        assertEquals("Neurology", created.specialization());

        Provider neuro = new Provider("Cid", "Park", "Neurology",
                "MONDAY,TUESDAY", LocalTime.of(9, 0), LocalTime.of(15, 0), 60);
        when(providers.findBySpecializationIgnoreCase("neurology"))
                .thenReturn(List.of(neuro));
        assertEquals(1, service.list("neurology").size());
        assertThrows(ResourceNotFoundException.class, () -> service.get(99L));
    }
}
