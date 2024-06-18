package com.mytutor.repositories;

import com.mytutor.constants.AppointmentStatus;
import com.mytutor.entities.Appointment;
import com.mytutor.entities.Timeslot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author vothimaihoa
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    @Query("SELECT a FROM Appointment a " +
            " WHERE a.tutor.id = :tutorId ")
    Page<Appointment> findAppointmentByTutorId(Integer tutorId, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            " WHERE a.student.id = :studentId ")
    Page<Appointment> findAppointmentByStudentId(Integer studentId, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            " WHERE a.tutor.id = :tutorId " +
            " AND a.status = :status")
    Page<Appointment> findAppointmentByTutorId(Integer tutorId, AppointmentStatus status, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            " WHERE a.student.id = :studentId " +
            " AND a.status = :status")
    Page<Appointment> findAppointmentByStudentId(Integer studentId, AppointmentStatus status, Pageable pageable);

    @Query("SELECT a FROM Appointment a "
            + " WHERE a.status = :status")
    Page<Appointment> findAppointments(AppointmentStatus status, Pageable pageable);


    @Query("SELECT DISTINCT a " +
            " FROM Appointment a JOIN a.timeslots t " +
            " WHERE t IN :timeslots " +
            " AND a.id != :appointmentId")
    List<Appointment> findAppointmentsWithOverlappingTimeslots(@Param("timeslots") List<Timeslot> timeslots, @Param("appointmentId") Integer appointmentId);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.student.id = :studentId")
    List<Appointment> findAppointmentsWithPendingPayment(@Param("studentId") Integer studentId, @Param("status") AppointmentStatus status);

    List<Appointment> findByStatusAndCreatedAtBefore(AppointmentStatus status, LocalDateTime dateTime);




}
