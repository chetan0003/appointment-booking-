package com.jfl.appointment.repository;


import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findById(Long id);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}
