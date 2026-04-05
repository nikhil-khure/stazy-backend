package com.stazy.backend.user.repository;

import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUserCodeIgnoreCase(String userCode);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUserCodeIgnoreCase(String userCode);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findWithRolesByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findWithRolesByUserCodeIgnoreCase(String userCode);

    List<User> findByPrimaryRoleCodeOrderByCreatedAtDesc(RoleName roleName);

    long countByPrimaryRoleCode(RoleName roleName);
}
