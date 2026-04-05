package com.stazy.backend.profile.repository;

import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, UUID> {

    Optional<OwnerProfile> findByUser(User user);
}
