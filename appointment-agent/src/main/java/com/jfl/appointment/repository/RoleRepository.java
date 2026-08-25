package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Role;
import com.jfl.appointment.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}