package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "Status is required.")
        AccountStatus status
) {
}
