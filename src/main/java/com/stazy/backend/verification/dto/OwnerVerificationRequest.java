package com.stazy.backend.verification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class OwnerVerificationRequest {

    private MultipartFile liveImage;

    private MultipartFile panImage;

    private MultipartFile userSignature;

    @NotBlank(message = "Owner name is required.")
    private String ownerName;

    @NotBlank(message = "PAN number is required.")
    private String panNumber;
}
