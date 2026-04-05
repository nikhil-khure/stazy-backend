package com.stazy.backend.admin.dto;

public record PlatformStatsResponse(
        long totalStudents,
        long totalOwners,
        long totalAdmins
) {
}
