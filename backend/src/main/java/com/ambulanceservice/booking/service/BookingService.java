package com.ambulanceservice.booking.service;

import com.ambulanceservice.booking.dto.BookingResponse;
import com.ambulanceservice.booking.dto.CreateBookingRequest;
import com.ambulanceservice.booking.dto.UpdateBookingStatusRequest;
import com.ambulanceservice.booking.entity.Booking;
import com.ambulanceservice.booking.entity.BookingStatus;
import com.ambulanceservice.dispatch.dto.AvailableDriverResponse;
import com.ambulanceservice.booking.repository.BookingRepository;
import com.ambulanceservice.dispatch.dto.AssignDriverRequest;
import com.ambulanceservice.common.ResourceNotFoundException;
import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.entity.UserStatus;
import com.ambulanceservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;

  @Transactional
  public BookingResponse createBooking(CreateBookingRequest request, String currentUserEmail, boolean attachAsRequester) {
    User currentUser = null;
    // If an authenticated user is involved, link their account either as the requester or as the staff recorder.
    if (currentUserEmail != null) {
      currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
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
        .requester(attachAsRequester ? currentUser : null)
        .recordedBy(attachAsRequester ? null : currentUser)
        .build();

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  public BookingResponse getBookingById(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
    return BookingResponse.fromEntity(booking);
  }

  public List<BookingResponse> getAvailableBookings(String driverEmail) {
    User driver = getUserByEmail(driverEmail);
    Long driverId = driver.getId();

    List<Booking> unassigned = bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.REQUESTED)
        .stream()
        .filter(booking -> booking.getAssignedDriver() == null)
        .toList();
    List<Booking> assignedToDriver = bookingRepository.findByStatusAndAssignedDriverIdOrderByCreatedAtDesc(BookingStatus.REQUESTED, driverId);

    return java.util.stream.Stream.concat(unassigned.stream(), assignedToDriver.stream())
        .distinct()
        .map(BookingResponse::fromEntity)
        .toList();
  }

  public List<BookingResponse> getOpenDispatchBookings() {
    return bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.REQUESTED)
        .stream().map(BookingResponse::fromEntity).toList();
  }

  public List<AvailableDriverResponse> getAvailableDriversForDispatch() {
    Set<Long> busyDriverIds = new HashSet<>(bookingRepository.findBusyDriverIds());

    return userRepository.findByRoleAndStatus(Role.ROLE_DRIVER, UserStatus.ACTIVE).stream()
        .filter(driver -> !busyDriverIds.contains(driver.getId()))
        .map(AvailableDriverResponse::fromEntity)
        .toList();
  }

  public List<BookingResponse> getMyBookings(String currentUserEmail) {
    User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (user.getRole() == Role.ROLE_DRIVER) {
      return bookingRepository.findByAssignedDriverIdOrderByCreatedAtDesc(user.getId())
          .stream().map(BookingResponse::fromEntity).toList();
    } else if (user.getRole() == Role.ROLE_DISPATCHER || user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_SUPER_ADMIN) {
      return bookingRepository.findByRecordedByIdOrderByCreatedAtDesc(user.getId())
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

    if (!bookingRepository.findActiveTripsByDriverEmail(driverEmail).isEmpty()) {
      throw new IllegalStateException("Driver already has an active trip.");
    }

    if (booking.getAssignedDriver() != null && !booking.getAssignedDriver().getId().equals(driver.getId())) {
      throw new IllegalStateException("This booking has already been reserved for another driver.");
    }

    booking.setAssignedDriver(driver);
    booking.setStatus(BookingStatus.ASSIGNED);

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  @Transactional
  public BookingResponse assignDriver(Long bookingId, AssignDriverRequest request, String dispatcherEmail) {
    ensureDispatchCapable(dispatcherEmail, "assign drivers");
    Booking booking = getRequestedBookingForDispatch(bookingId, "assign");

    User driver = userRepository.findById(request.driverId())
        .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.driverId()));

    if (driver.getRole() != Role.ROLE_DRIVER) {
      throw new IllegalArgumentException("Selected user is not a driver.");
    }

    if (driver.getStatus() != UserStatus.ACTIVE) {
      throw new IllegalStateException("Only active drivers can receive dispatch assignments.");
    }

    if (!bookingRepository.findActiveTripsByDriverEmail(driver.getEmail()).isEmpty()) {
      throw new IllegalStateException("Selected driver already has an active trip.");
    }

    booking.setAssignedDriver(driver);
    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  @Transactional
  public BookingResponse unassignDriver(Long bookingId, String dispatcherEmail) {
    ensureDispatchCapable(dispatcherEmail, "unassign drivers");
    Booking booking = getRequestedBookingForDispatch(bookingId, "unassign");

    if (booking.getAssignedDriver() == null) {
      throw new IllegalStateException("This booking is not currently reserved for any driver.");
    }

    booking.setAssignedDriver(null);
    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  @Transactional
  public BookingResponse updateStatus(Long bookingId, UpdateBookingStatusRequest request, String userEmail) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    ensureAssignedDriver(booking, userEmail);
    validateDriverStatusTransition(booking.getStatus(), request.status());

    booking.setStatus(request.status());

    if (request.status() == BookingStatus.COMPLETED) {
      booking.setCompletedAt(LocalDateTime.now());
    }

    if (request.status() == BookingStatus.CANCELLED) {
      booking.setCancellationReason(request.cancellationReason());
    }

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  private User getUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }

  private boolean isDispatchCapable(Role role) {
    return role == Role.ROLE_DISPATCHER || role == Role.ROLE_ADMIN || role == Role.ROLE_SUPER_ADMIN;
  }

  private void ensureDispatchCapable(String userEmail, String action) {
    User dispatcher = getUserByEmail(userEmail);
    if (!isDispatchCapable(dispatcher.getRole())) {
      throw new IllegalStateException("Only dispatch staff can " + action + ".");
    }
  }

  private Booking getRequestedBookingForDispatch(Long bookingId, String action) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    if (booking.getStatus() != BookingStatus.REQUESTED) {
      throw new IllegalStateException("Only requested bookings can be updated by dispatch to " + action + " a driver.");
    }

    return booking;
  }

  private void ensureAssignedDriver(Booking booking, String userEmail) {
    if (booking.getAssignedDriver() == null || !booking.getAssignedDriver().getEmail().equalsIgnoreCase(userEmail)) {
      throw new IllegalStateException("Only the assigned driver can update this booking.");
    }
  }

  private void validateDriverStatusTransition(BookingStatus currentStatus, BookingStatus requestedStatus) {
    boolean valid = switch (currentStatus) {
      case ASSIGNED -> requestedStatus == BookingStatus.IN_TRANSIT || requestedStatus == BookingStatus.CANCELLED;
      case IN_TRANSIT -> requestedStatus == BookingStatus.ARRIVED || requestedStatus == BookingStatus.CANCELLED;
      case ARRIVED -> requestedStatus == BookingStatus.COMPLETED || requestedStatus == BookingStatus.CANCELLED;
      default -> false;
    };

    if (!valid) {
      throw new IllegalStateException("Invalid booking status transition: " + currentStatus + " -> " + requestedStatus);
    }
  }
}
