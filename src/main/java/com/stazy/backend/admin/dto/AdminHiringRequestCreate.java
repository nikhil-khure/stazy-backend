package com.stazy.backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AdminHiringRequestCreate {

    @NotBlank(message = "Full name is required.")
    private String fullName;

    @NotBlank(message = "Mobile number is required.")
    private String mobileNumber;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    private String email;

    private MultipartFile resume;
}
