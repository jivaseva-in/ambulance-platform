package com.ambulanceservice.booking.repository;

import com.ambulanceservice.booking.entity.Booking;
import com.ambulanceservice.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
  List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
  List<Booking> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
  List<Booking> findByAssignedDriverIdOrderByCreatedAtDesc(Long driverId);

  // Active trip query for assigned driver using ASSIGNED, IN_TRANSIT, and ARRIVED
  @Query("SELECT b FROM Booking b WHERE b.assignedDriver.email = :driverEmail " +
      "AND b.status IN (com.ambulanceservice.booking.entity.BookingStatus.ASSIGNED, " +
      "com.ambulanceservice.booking.entity.BookingStatus.IN_TRANSIT, " +
      "com.ambulanceservice.booking.entity.BookingStatus.ARRIVED) " +
      "ORDER BY b.createdAt DESC")
  List<Booking> findActiveTripsByDriverEmail(@Param("driverEmail") String driverEmail);
}
