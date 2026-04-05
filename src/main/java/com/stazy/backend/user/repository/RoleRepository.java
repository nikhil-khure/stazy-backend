package com.stazy.backend.user.repository;

import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.user.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(RoleName code);
}
