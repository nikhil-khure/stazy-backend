package com.stazy.backend.profile.repository;

import com.stazy.backend.profile.entity.AdminProfile;
import com.stazy.backend.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, UUID> {

    Optional<AdminProfile> findByUser(User user);
}
