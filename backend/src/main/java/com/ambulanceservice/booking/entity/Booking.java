package com.ambulanceservice.booking.entity;

import com.ambulanceservice.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Contact details for immediate phone coordination
  @Column(nullable = false, length = 100)
  private String contactName;

  @Column(nullable = false, length = 20)
  private String contactPhone;

  // Locations
  @Column(nullable = false, length = 255)
  private String pickupAddress;

  private Double pickupLatitude;
  private Double pickupLongitude;

  @Column(nullable = false, length = 255)
  private String destinationAddress;

  private Double destinationLatitude;
  private Double destinationLongitude;

  // Medical assessment
  @Column(length = 500)
  private String patientCondition;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AmbulanceType ambulanceType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BookingChannel bookingChannel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BookingStatus status;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requester_id")
  private User requester; // Nullable if booked via phone by dispatcher for unregistered caller

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recorded_by_id")
  private User recordedBy; // Staff user who entered the booking on behalf of the caller

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "driver_id")
  private User assignedDriver; // Null until accepted by a driver

  @Column(length = 255)
  private String cancellationReason;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  private LocalDateTime completedAt;

  @Column(name = "driver_latitude")
  private Double driverLatitude;

  @Column(name = "driver_longitude")
  private Double driverLongitude;

  @Column(name = "last_location_update")
  private LocalDateTime lastLocationUpdate;
}
