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
import com.ambulanceservice.common.GeoCodingService;
import com.ambulanceservice.common.GeoCodingService.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;
  private final GeoCodingService geoCodingService;

  @Transactional
  public BookingResponse createBooking(CreateBookingRequest request, String currentUserEmail, boolean attachAsRequester) {
    CoordinatePair pickup = resolveCoordinates(
        request.pickupLatitude(),
        request.pickupLongitude(),
        request.pickupAddress(),
        "Pickup");
    CoordinatePair destination = resolveCoordinates(
        request.destinationLatitude(),
        request.destinationLongitude(),
        request.destinationAddress(),
        "Destination");

    User currentUser = null;
    // If an authenticated user is involved, link their account either as the requester or as the staff recorder.
    if (currentUserEmail != null) {
      currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
    }

    Booking booking = Booking.builder()
        .contactName(request.contactName())
        .contactPhone(request.contactPhone())
        .pickupAddress(request.pickupAddress())
        .pickupLatitude(pickup.latitude())
        .pickupLongitude(pickup.longitude())
        .destinationAddress(request.destinationAddress())
        .destinationLatitude(destination.latitude())
        .destinationLongitude(destination.longitude())
        .patientCondition(request.patientCondition())
        .ambulanceType(request.ambulanceType())
        .bookingChannel(request.bookingChannel())
        .status(BookingStatus.REQUESTED)
        .requester(attachAsRequester ? currentUser : null)
        .recordedBy(attachAsRequester ? null : currentUser)
        .build();

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  private CoordinatePair resolveCoordinates(Double latitude, Double longitude, String address, String label) {
    if (latitude != null || longitude != null) {
      if (latitude == null || longitude == null) {
        throw new IllegalArgumentException(label + " latitude and longitude must both be provided together.");
      }
      validateCoordinates(latitude, longitude, label + " coordinates are invalid.");
      return new CoordinatePair(latitude, longitude);
    }

    if (address == null || address.isBlank()) {
      throw new IllegalArgumentException(label + " address is required when no coordinates are supplied.");
    }

    GeoPoint geoPoint = geoCodingService.geocode(address).orElse(null);
    if (geoPoint == null) {
      return new CoordinatePair(null, null);
    }

    validateCoordinates(geoPoint.latitude(), geoPoint.longitude(), label + " resolved coordinates are invalid.");
    return new CoordinatePair(geoPoint.latitude(), geoPoint.longitude());
  }

  private record CoordinatePair(Double latitude, Double longitude) {
  }

  private void validateCoordinates(Double latitude, Double longitude, String message) {
    if (latitude == null || longitude == null) {
      throw new IllegalArgumentException(message);
    }
    if (latitude.isNaN() || longitude.isNaN() || latitude.isInfinite() || longitude.isInfinite()) {
      throw new IllegalArgumentException(message);
    }
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
      throw new IllegalArgumentException(message);
    }
  }

  public BookingResponse getBookingById(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
    return BookingResponse.fromEntity(booking);
  }

  public List<BookingResponse> getAvailableBookings(String driverEmail) {
    User driver = getUserByEmail(driverEmail);
    Long driverId = driver.getId();

    List<Booking> available = java.util.stream.Stream.of(
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.REQUESTED),
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.REASSIGNMENT_PENDING),
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.NEGOTIATION_FAILED),
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.CANCEL_REQUESTED)
        )
        .flatMap(List::stream)
        .filter(booking -> booking.getAssignedDriver() == null)
        .toList();

    List<Booking> driverHeld = bookingRepository.findByStatusAndAssignedDriverIdOrderByCreatedAtDesc(BookingStatus.DRIVER_HELD, driverId);
    List<Booking> driverConfirmed = bookingRepository.findByStatusAndAssignedDriverIdOrderByCreatedAtDesc(BookingStatus.CONFIRMED, driverId);

    return java.util.stream.Stream.concat(
            java.util.stream.Stream.concat(available.stream(), driverHeld.stream()),
            driverConfirmed.stream())
        .distinct()
        .map(BookingResponse::fromEntity)
        .toList();
  }

  public List<BookingResponse> getOpenDispatchBookings() {
    return java.util.stream.Stream.of(
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.REQUESTED),
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.REASSIGNMENT_PENDING),
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.NEGOTIATION_FAILED),
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.CANCEL_REQUESTED))
        .flatMap(List::stream)
        .distinct()
        .map(BookingResponse::fromEntity)
        .toList();
  }

  @Transactional
  public BookingResponse cancelBookingRequest(Long bookingId, String driverEmail, String reason) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    ensureAssignedDriver(booking, driverEmail);
    if (booking.getStatus() == BookingStatus.CANCEL_REQUESTED || booking.getStatus() == BookingStatus.CANCELLED
        || booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.EXPIRED) {
      throw new IllegalStateException("This booking cannot be cancelled again.");
    }

    if (booking.getStatus() == BookingStatus.IN_TRANSIT || booking.getStatus() == BookingStatus.ARRIVED) {
      throw new IllegalStateException("Cancellation after trip start requires dispatcher approval.");
    }

    booking.setAssignedDriver(null);
    booking.setStatus(BookingStatus.REASSIGNMENT_PENDING);
    booking.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Driver cancelled by mistake");

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  @Transactional
  public BookingResponse requeueBooking(Long bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    if (booking.getStatus() != BookingStatus.NEGOTIATION_FAILED
        && booking.getStatus() != BookingStatus.CANCEL_REQUESTED
        && booking.getStatus() != BookingStatus.REASSIGNMENT_PENDING) {
      throw new IllegalStateException("This booking cannot be re-queued in its current state.");
    }

    booking.setAssignedDriver(null);
    booking.setStatus(BookingStatus.REASSIGNMENT_PENDING);
    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  @Transactional
  public BookingResponse failNegotiation(Long bookingId, String driverEmail, String reason) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    ensureAssignedDriver(booking, driverEmail);
    if (booking.getStatus() != BookingStatus.DRIVER_HELD && booking.getStatus() != BookingStatus.CONFIRMED) {
      throw new IllegalStateException("Only a held or confirmed booking can fail negotiation.");
    }

    booking.setAssignedDriver(null);
    booking.setStatus(BookingStatus.REASSIGNMENT_PENDING);
    booking.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Price negotiation failed");

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
    booking.setStatus(BookingStatus.CONFIRMED);
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

  private Booking getRequestedBookingForDispatch(Long bookingId, String action) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    if (booking.getStatus() != BookingStatus.REQUESTED && booking.getStatus() != BookingStatus.REASSIGNMENT_PENDING) {
      throw new IllegalStateException("Only requested or re-assignment pending bookings can be updated by dispatch to " + action + " a driver.");
    }

    return booking;
  }

  private void validateDriverStatusTransition(BookingStatus currentStatus, BookingStatus requestedStatus) {
    boolean valid = switch (currentStatus) {
      case REQUESTED -> requestedStatus == BookingStatus.DRIVER_HELD || requestedStatus == BookingStatus.CANCELLED;
      case DRIVER_HELD -> requestedStatus == BookingStatus.CONFIRMED || requestedStatus == BookingStatus.CANCEL_REQUESTED
          || requestedStatus == BookingStatus.CANCELLED;
      case CONFIRMED -> requestedStatus == BookingStatus.IN_TRANSIT || requestedStatus == BookingStatus.CANCEL_REQUESTED
          || requestedStatus == BookingStatus.CANCELLED;
      case ASSIGNED -> requestedStatus == BookingStatus.IN_TRANSIT || requestedStatus == BookingStatus.CANCEL_REQUESTED
          || requestedStatus == BookingStatus.CANCELLED;
      case IN_TRANSIT -> requestedStatus == BookingStatus.ARRIVED || requestedStatus == BookingStatus.CANCEL_REQUESTED
          || requestedStatus == BookingStatus.CANCELLED;
      case ARRIVED -> requestedStatus == BookingStatus.COMPLETED || requestedStatus == BookingStatus.CANCEL_REQUESTED
          || requestedStatus == BookingStatus.CANCELLED;
      default -> false;
    };

    if (!valid) {
      throw new IllegalStateException("Invalid booking status transition: " + currentStatus + " -> " + requestedStatus);
    }
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
    if (!activeTrips.isEmpty()) {
      return Optional.of(BookingResponse.fromEntity(activeTrips.get(0)));
    }

    List<Booking> assignedTrips = bookingRepository.findByAssignedDriverIdOrderByCreatedAtDesc(
        userRepository.findByEmail(driverEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + driverEmail))
            .getId());

    return assignedTrips.stream()
        .filter(booking -> booking.getStatus() == BookingStatus.DRIVER_HELD
            || booking.getStatus() == BookingStatus.CONFIRMED
            || booking.getStatus() == BookingStatus.ASSIGNED
            || booking.getStatus() == BookingStatus.IN_TRANSIT
            || booking.getStatus() == BookingStatus.ARRIVED)
        .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
        .findFirst()
        .map(BookingResponse::fromEntity);
  }

  public BookingResponse getBookingByPhone(String phoneNumber) {
    String normalized = normalizePhone(phoneNumber);
    if (normalized == null || normalized.isBlank()) {
      throw new IllegalArgumentException("Phone number is required");
    }

    List<Booking> matches = bookingRepository.findByContactPhoneContainingOrderByCreatedAtDesc(normalized);
    if (matches.isEmpty()) {
      throw new ResourceNotFoundException("No booking found for phone number: " + phoneNumber);
    }

    return BookingResponse.fromEntity(matches.get(0));
  }

  private String normalizePhone(String phoneNumber) {
    if (phoneNumber == null) {
      return null;
    }

    String digits = phoneNumber.replaceAll("[^0-9]", "");
    if (digits.isBlank()) {
      return null;
    }
    return digits;
  }

  public BookingResponse unassignDriver(Long bookingId, String dispatcherEmail) {
    ensureDispatchCapable(dispatcherEmail, "unassign drivers");
    Booking booking = getRequestedBookingForDispatch(bookingId, "unassign");

    if (booking.getAssignedDriver() == null) {
      throw new IllegalStateException("This booking is not currently reserved for any driver.");
    }

    booking.setAssignedDriver(null);
    booking.setStatus(BookingStatus.REQUESTED);
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

  private void ensureAssignedDriver(Booking booking, String userEmail) {
    if (booking.getAssignedDriver() == null || !booking.getAssignedDriver().getEmail().equalsIgnoreCase(userEmail)) {
      throw new IllegalStateException("Only the assigned driver can update this booking.");
    }
  }

  @Transactional
  public BookingResponse acceptBooking(Long bookingId, String driverEmail) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED
        || booking.getStatus() == BookingStatus.EXPIRED) {
      throw new IllegalStateException("This booking is no longer active.");
    }

    if (booking.getStatus() != BookingStatus.REQUESTED && booking.getStatus() != BookingStatus.REASSIGNMENT_PENDING) {
      throw new IllegalStateException("Booking is not available for a new driver hold.");
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
    booking.setStatus(BookingStatus.DRIVER_HELD);

    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

  @Transactional
  public BookingResponse confirmBooking(Long bookingId, String driverEmail) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

    ensureAssignedDriver(booking, driverEmail);
    if (booking.getStatus() != BookingStatus.DRIVER_HELD) {
      throw new IllegalStateException("Only a held booking can be confirmed.");
    }

    booking.setStatus(BookingStatus.CONFIRMED);
    return BookingResponse.fromEntity(bookingRepository.save(booking));
  }

}
