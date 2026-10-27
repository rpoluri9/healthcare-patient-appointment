package com.healthcare.appointment.repository;

import com.healthcare.appointment.model.Appointment;
import com.healthcare.appointment.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByProviderId(Long providerId);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    /** BOOKED appointments of one provider overlapping [start, end). */
    @Query("SELECT a FROM Appointment a WHERE a.provider.id = :providerId "
            + "AND a.status = 'BOOKED' "
            + "AND a.startTime < :end AND a.endTime > :start")
    List<Appointment> findProviderOverlaps(@Param("providerId") Long providerId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    /** BOOKED appointments of one patient overlapping [start, end). */
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId "
            + "AND a.status = 'BOOKED' "
            + "AND a.startTime < :end AND a.endTime > :start")
    List<Appointment> findPatientOverlaps(@Param("patientId") Long patientId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    List<Appointment> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);
}
