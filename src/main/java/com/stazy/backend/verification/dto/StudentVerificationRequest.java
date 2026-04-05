package com.stazy.backend.verification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class StudentVerificationRequest {

    private MultipartFile liveImage;

    private MultipartFile idCardImage;

    @NotBlank(message = "College name is required.")
    private String collegeName;

    @NotBlank(message = "PRN is required.")
    private String prn;
}
