package com.ambulanceservice.security;

import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
    // Allows login using either email or phone
    User user = userRepository.findByEmail(identifier)
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword(),
        user.isActive(),
        true, true, true,
        Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
    );
  }
}
