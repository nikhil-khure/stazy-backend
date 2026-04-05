package com.stazy.backend.complaint.entity;

import com.stazy.backend.common.entity.CreatedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "complaint_attachments")
public class ComplaintAttachment extends CreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_message_id")
    private ComplaintMessage complaintMessage;

    private String url;

    private String publicId;

    private String mimeType;

    private Long fileSizeBytes;
}
