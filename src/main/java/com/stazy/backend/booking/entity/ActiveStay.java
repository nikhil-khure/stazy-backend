package com.stazy.backend.booking.entity;

import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.ActiveStayStatus;
import com.stazy.backend.common.enums.PaymentStatus;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "active_stays")
public class ActiveStay extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_request_id", nullable = false, unique = true)
    private BookingRequest bookingRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User studentUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    private String roomCode;

    @Column(nullable = false)
    private LocalDate joinDate;

    private LocalDate currentMonthStart;

    private LocalDate currentMonthEnd;

    @Column(nullable = false)
    private BigDecimal monthlyRent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStayStatus status = ActiveStayStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus currentPaymentStatus = PaymentStatus.UNPAID;

    private LocalDate nextDueDate;

    @Column(columnDefinition = "TEXT")
    private String reminderMessage;
}
