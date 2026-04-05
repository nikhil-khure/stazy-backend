package com.stazy.backend.profile.repository;

import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    Optional<StudentProfile> findByUser(User user);
}
