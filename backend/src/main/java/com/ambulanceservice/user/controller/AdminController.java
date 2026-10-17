package com.ambulanceservice.user.controller;

import com.ambulanceservice.user.dto.UserResponse;
import com.ambulanceservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // Secures the whole controller
public class AdminController {

  private final UserService userService;

  @GetMapping("/users/pending")
  public ResponseEntity<List<UserResponse>> getPendingApprovalUsers(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.getPendingApprovalUsers(userDetails.getUsername()));
  }

  @PutMapping("/users/{id}/approve")
  public ResponseEntity<UserResponse> approveUser(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.approveUser(id, userDetails.getUsername()));
  }

  @PutMapping("/users/{id}/reject")
  public ResponseEntity<UserResponse> rejectUser(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.rejectUser(id, userDetails.getUsername()));
  }

  @PutMapping("/users/{id}/suspend")
  public ResponseEntity<UserResponse> suspendUser(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.suspendUser(id, userDetails.getUsername()));
  }

  @PutMapping("/users/{id}/reactivate")
  public ResponseEntity<UserResponse> reactivateUser(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.reactivateUser(id, userDetails.getUsername()));
  }

  @GetMapping("/users")
  public ResponseEntity<List<UserResponse>> getManageableUsers(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.getManageableUsers(userDetails.getUsername()));
  }
}
