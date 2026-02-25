package com.workcopilot.user.repository;

import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    List<User> findByStatusOrderByCreatedAtAsc(UserStatus status);
}
