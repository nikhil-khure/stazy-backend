package com.stazy.backend.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ComplaintActionRequest {

    @NotBlank(message = "Message is required.")
    private String message;

    private MultipartFile[] attachments;
}
