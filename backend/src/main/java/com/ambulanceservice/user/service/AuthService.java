package com.ambulanceservice.user.service;

import com.ambulanceservice.security.JwtUtils;
import com.ambulanceservice.user.dto.*;
import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtUtils;
  private final UserDetailsService userDetailsService;

  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("Email is already registered");
    }
    if (userRepository.existsByPhone(request.phone())) {
      throw new IllegalArgumentException("Phone number is already registered");
    }

    User user = User.builder()
        .fullName(request.fullName())
        .email(request.email())
        .phone(request.phone())
        .password(passwordEncoder.encode(request.password()))
        .role(request.role())
        .active(true)
        .build();

    userRepository.save(user);

    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    String token = jwtUtils.generateToken(userDetails);

    return AuthResponse.of(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
  }

  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password())
    );

    User user = userRepository.findByEmail(request.username())
        .or(() -> userRepository.findByPhone(request.username()))
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    String token = jwtUtils.generateToken(userDetails);

    return AuthResponse.of(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
  }
}
