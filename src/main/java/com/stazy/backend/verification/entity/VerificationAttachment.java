package com.stazy.backend.verification.entity;

import com.stazy.backend.common.entity.CreatedEntity;
import com.stazy.backend.common.enums.VerificationAttachmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "verification_attachments")
public class VerificationAttachment extends CreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_request_id", nullable = false)
    private VerificationRequest verificationRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationAttachmentType attachmentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    private String publicId;

    private String mimeType;

    private Long fileSizeBytes;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
