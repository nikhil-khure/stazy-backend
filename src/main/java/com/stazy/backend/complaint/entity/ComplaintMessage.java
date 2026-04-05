package com.stazy.backend.complaint.entity;

import com.stazy.backend.common.entity.CreatedEntity;
import com.stazy.backend.common.enums.ComplaintMessageType;
import com.stazy.backend.user.entity.User;
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
@Table(name = "complaint_messages")
public class ComplaintMessage extends CreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User authorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private ComplaintMessageType messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
}
