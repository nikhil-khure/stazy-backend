package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.enums.EmployeeStatus;
import java.util.UUID;

public record ConnectedAdminResponse(
        UUID userId,
        String userCode,
        String displayName,
        String email,
        String cityName,
        AccountStatus accountStatus,
        EmployeeStatus employeeStatus,
        boolean canManageAllCities
) {
}
