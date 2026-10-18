package com.ambulanceservice.user.repository;

import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.UserStatus;
import com.ambulanceservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
  Optional<User> findByPhone(String phone);
  boolean existsByEmail(String email);
  boolean existsByPhone(String phone);
  boolean existsByRole(Role role);
  List<User> findByRoleAndStatus(Role role, UserStatus status);
  List<User> findByStatus(UserStatus status);
}
