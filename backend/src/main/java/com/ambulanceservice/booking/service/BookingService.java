package com.ambulanceservice.booking.service;

import com.ambulanceservice.booking.dto.BookingResponse;
import com.ambulanceservice.booking.dto.CreateBookingRequest;
import com.ambulanceservice.booking.dto.UpdateBookingStatusRequest;
import com.ambulanceservice.booking.entity.Booking;
import com.ambulanceservice.booking.entity.BookingStatus;
import com.ambulanceservice.booking.repository.BookingRepository;
import com.ambulanceservice.common.ResourceNotFoundException;
import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;

  @Transactional
  public BookingResponse createBooking(CreateBookingRequest request, String currentUserEmail) {
    User requester = null;
    // If an authenticated user is placing the booking, link their account; otherwise, leave null (Guest)
    if (currentUserEmail != null) {
      requester = userRepository.findByEmail(currentUserEmail).orElse(null);
    }

    Booking booking = Booking.builder()
        .contactName(request.contactName())
        .contactPhone(request.contactPhone())
        .pickupAddress(request.pickupAddress())
        .pickupLatitude(request.pickupLatitude())
        .pickupLongitude(request.pickupLongitude())
        .destinationAddress(request.destinationAddress())
        .destinationLatitude(request.destinationLatitude())
        .destinationLongitude(request.destinationLongitude())
        .patientCondition(request.patientCondition())
        .ambulanceType(request.ambulanceType())
        .bookingChannel(request.bookingChannel())
        .status(BookingStatus.REQUESTED)
        .requester(requester)
        .build();

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  public BookingResponse getBookingById(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
    return BookingResponse.fromEntity(booking);
  }

  public List<BookingResponse> getAvailableBookings() {
    return bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.REQUESTED)
        .stream().map(BookingResponse::fromEntity).toList();
  }

  public List<BookingResponse> getMyBookings(String currentUserEmail) {
    User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (user.getRole() == Role.ROLE_DRIVER) {
      return bookingRepository.findByAssignedDriverIdOrderByCreatedAtDesc(user.getId())
          .stream().map(BookingResponse::fromEntity).toList();
    } else {
      return bookingRepository.findByRequesterIdOrderByCreatedAtDesc(user.getId())
          .stream().map(BookingResponse::fromEntity).toList();
    }
  }

  public List<BookingResponse> getAllBookings() {
    return bookingRepository.findAll()
        .stream().map(BookingResponse::fromEntity).toList();
  }

  public Optional<BookingResponse> getActiveTripForDriver(String driverEmail) {
    List<Booking> activeTrips = bookingRepository.findActiveTripsByDriverEmail(driverEmail);
    if (activeTrips.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(BookingResponse.fromEntity(activeTrips.get(0)));
  }

  @Transactional
  public BookingResponse acceptBooking(Long bookingId, String driverEmail) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    if (booking.getStatus() != BookingStatus.REQUESTED) {
      throw new IllegalStateException("Booking is already assigned or closed");
    }

    User driver = userRepository.findByEmail(driverEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + driverEmail));

    booking.setAssignedDriver(driver);
    booking.setStatus(BookingStatus.ASSIGNED);

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  @Transactional
  public BookingResponse updateStatus(Long bookingId, UpdateBookingStatusRequest request, String userEmail) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    booking.setStatus(request.status());

    if (request.status() == BookingStatus.COMPLETED) {
      booking.setCompletedAt(LocalDateTime.now());
    }

    if (request.status() == BookingStatus.CANCELLED) {
      booking.setCancellationReason(request.cancellationReason());
    }

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }
}
