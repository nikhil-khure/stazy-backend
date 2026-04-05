package com.stazy.backend.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ComplaintCreateRequest {

    @NotBlank(message = "Target user ID is required.")
    private String againstUserCode;

    @NotBlank(message = "Complaint title is required.")
    private String title;

    @NotBlank(message = "Complaint description is required.")
    private String description;

    private UUID relatedListingId;

    private UUID relatedStayId;

    private MultipartFile[] attachments;
}
