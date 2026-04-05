package com.stazy.backend.admin.repository;

import com.stazy.backend.admin.entity.AdminQuery;
import com.stazy.backend.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminQueryRepository extends JpaRepository<AdminQuery, UUID> {

    List<AdminQuery> findByAdminUserOrderByCreatedAtDesc(User adminUser);
}
