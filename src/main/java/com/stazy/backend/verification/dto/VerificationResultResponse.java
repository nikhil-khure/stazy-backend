package com.stazy.backend.verification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.stazy.backend.common.enums.VerificationStatus;
import com.stazy.backend.common.enums.VerificationType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VerificationResultResponse(
        UUID verificationId,
        VerificationType verificationType,
        VerificationStatus status,
        boolean verified,
        String message,
        JsonNode acceptedParameters,
        JsonNode rejectedParameters,
        JsonNode rawResponse,
        OffsetDateTime createdAt
) {
}
