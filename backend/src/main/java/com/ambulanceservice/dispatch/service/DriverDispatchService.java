package com.ambulanceservice.dispatch.service;

import com.ambulanceservice.dispatch.dto.AvailableDriverResponse;
import com.ambulanceservice.dispatch.dto.DispatchDriverCandidateResponse;
import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.entity.UserStatus;
import com.ambulanceservice.user.repository.UserRepository;
import com.ambulanceservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverDispatchService {

  private final UserRepository userRepository;
  private final UserService userService;

  public List<AvailableDriverResponse> getAvailableDriversForDispatch() {
    return userRepository.findByRoleAndStatus(Role.ROLE_DRIVER, UserStatus.ACTIVE).stream()
        .filter(userService::isDriverAvailableForDispatch)
        .sorted(Comparator.comparingDouble(this::distanceFromDefaultZero).thenComparing(User::getLastLocationUpdate, Comparator.nullsLast(Comparator.reverseOrder())))
        .limit(5)
        .map(AvailableDriverResponse::fromEntity)
        .toList();
  }

  private double distanceFromDefaultZero(User driver) {
    if (driver.getLatitude() == null || driver.getLongitude() == null) {
      return Double.MAX_VALUE;
    }
    return Math.hypot(driver.getLatitude(), driver.getLongitude());
  }

  public List<DispatchDriverCandidateResponse> getNearestAvailableDrivers(
      Double pickupLatitude,
      Double pickupLongitude,
      Double destinationLatitude,
      Double destinationLongitude) {

    final double referenceLatitude = pickupLatitude != null ? pickupLatitude : 0.0;
    final double referenceLongitude = pickupLongitude != null ? pickupLongitude : 0.0;

    return userRepository.findByRoleAndStatus(Role.ROLE_DRIVER, UserStatus.ACTIVE).stream()
        .filter(userService::isDriverAvailableForDispatch)
        .filter(driver -> driver.getLatitude() != null && driver.getLongitude() != null)
        .map(driver -> DispatchDriverCandidateResponse.fromEntity(
            driver,
            calculateDistance(driver.getLatitude(), driver.getLongitude(), referenceLatitude, referenceLongitude)
        ))
        .sorted(Comparator.comparingDouble(DispatchDriverCandidateResponse::distanceKm))
        .limit(5)
        .toList();
  }

  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return 6371.0 * c;
  }

  public List<DispatchDriverCandidateResponse> getNearestAvailableDriversWithDistance(
      Double pickupLatitude,
      Double pickupLongitude) {

    final double referenceLatitude = pickupLatitude != null ? pickupLatitude : 0.0;
    final double referenceLongitude = pickupLongitude != null ? pickupLongitude : 0.0;

    return userRepository.findByRoleAndStatus(Role.ROLE_DRIVER, UserStatus.ACTIVE).stream()
        .filter(userService::isDriverAvailableForDispatch)
        .filter(driver -> driver.getLatitude() != null && driver.getLongitude() != null)
        .map(driver -> DispatchDriverCandidateResponse.fromEntity(
            driver,
            calculateDistance(driver.getLatitude(), driver.getLongitude(), referenceLatitude, referenceLongitude)
        ))
        .sorted(Comparator.comparingDouble(DispatchDriverCandidateResponse::distanceKm))
        .limit(5)
        .toList();
  }
}
