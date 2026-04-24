package com.stazy.backend.booking.service;

import com.stazy.backend.booking.dto.ActiveStayResponse;
import com.stazy.backend.booking.dto.BookingCreateRequest;
import com.stazy.backend.booking.dto.BookingRequestResponse;
import com.stazy.backend.booking.dto.BookingReviewRequest;
import com.stazy.backend.booking.dto.CancelRequestCreateRequest;
import com.stazy.backend.booking.dto.CancelRequestReviewRequest;
import com.stazy.backend.booking.dto.RentPaymentResponse;
import com.stazy.backend.booking.dto.RentPaymentUpdateRequest;
import com.stazy.backend.booking.dto.StayCancelResponse;
import com.stazy.backend.booking.entity.ActiveStay;
import com.stazy.backend.booking.entity.BookingRequest;
import com.stazy.backend.booking.entity.RentPayment;
import com.stazy.backend.booking.entity.StayCancelRequest;
import com.stazy.backend.booking.repository.ActiveStayRepository;
import com.stazy.backend.booking.repository.BookingRequestRepository;
import com.stazy.backend.booking.repository.RentPaymentRepository;
import com.stazy.backend.booking.repository.StayCancelRequestRepository;
import com.stazy.backend.common.events.RealtimeEventPublisher;
import com.stazy.backend.common.enums.ActiveStayStatus;
import com.stazy.backend.common.enums.BookingRequestStatus;
import com.stazy.backend.common.enums.CancelRequestStatus;
import com.stazy.backend.common.enums.NotificationType;
import com.stazy.backend.common.enums.PaymentStatus;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.complaint.repository.ComplaintRepository;
import com.stazy.backend.admin.repository.FeedbackRepository;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.listing.repository.ListingRepository;
import com.stazy.backend.notification.service.NotificationService;
import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.profile.repository.StudentProfileRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.service.CurrentUserService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private static final Set<BookingRequestStatus> ACTIVE_REQUEST_STATUSES = Set.of(
            BookingRequestStatus.PENDING,
            BookingRequestStatus.ACCEPTED
    );

    private final BookingRequestRepository bookingRequestRepository;
    private final ActiveStayRepository activeStayRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final StayCancelRequestRepository stayCancelRequestRepository;
    private final ListingRepository listingRepository;
    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final NotificationService notificationService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ComplaintRepository complaintRepository;
    private final FeedbackRepository feedbackRepository;

    public BookingService(
            BookingRequestRepository bookingRequestRepository,
            ActiveStayRepository activeStayRepository,
            RentPaymentRepository rentPaymentRepository,
            StayCancelRequestRepository stayCancelRequestRepository,
            ListingRepository listingRepository,
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            NotificationService notificationService,
            RealtimeEventPublisher realtimeEventPublisher,
            ComplaintRepository complaintRepository,
            FeedbackRepository feedbackRepository
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.activeStayRepository = activeStayRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.stayCancelRequestRepository = stayCancelRequestRepository;
        this.listingRepository = listingRepository;
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.notificationService = notificationService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.complaintRepository = complaintRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public BookingRequestResponse createRequest(UUID studentId, UUID listingId, BookingCreateRequest request) {
        User student = currentUserService.requireUser(studentId);
        if (student.getPrimaryRoleCode() != RoleName.STUDENT) {
            throw new BadRequestException("Only students can request bookings.");
        }
        ensureVerifiedAccess(student);
        
        // Get student profile to check gender
        StudentProfile studentProfile = studentProfileRepository.findByUser(student)
                .orElseThrow(() -> new BadRequestException("Student profile not found."));
        
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        
        // Check if listing is available
        if (listing.getAvailableCapacity() <= 0) {
            throw new BadRequestException("This listing is currently full.");
        }
        
        // Validation 1: Check if student already has an active booking or stay
        boolean hasActiveBooking = bookingRequestRepository.existsByStudentUserAndStatusIn(
                student, 
                List.of(BookingRequestStatus.PENDING, BookingRequestStatus.ACCEPTED)
        );
        boolean hasActiveStay = activeStayRepository.existsByStudentUserAndStatus(
                student, 
                ActiveStayStatus.ACTIVE
        );
        
        if (hasActiveBooking || hasActiveStay) {
            throw new BadRequestException("You have already booked one room listing. You can book another room only after you leave your current room.");
        }
        
        // Validation 2: Gender-based booking restriction
        if (studentProfile.getGender() != null && listing.getGenderCategory() != null) {
            com.stazy.backend.common.enums.GenderCategory listingCategory = listing.getGenderCategory();
            com.stazy.backend.common.enums.Gender studentGender = studentProfile.getGender();
            
            // If listing is MALE only and student is FEMALE
            if (listingCategory == com.stazy.backend.common.enums.GenderCategory.MALE && 
                studentGender == com.stazy.backend.common.enums.Gender.FEMALE) {
                throw new BadRequestException("Only boys are allowed to book this room listing.");
            }
            
            // If listing is FEMALE only and student is MALE
            if (listingCategory == com.stazy.backend.common.enums.GenderCategory.FEMALE && 
                studentGender == com.stazy.backend.common.enums.Gender.MALE) {
                throw new BadRequestException("Only girls are allowed to book this room listing.");
            }
        }
        
        // Check if already requested this specific listing
        if (bookingRequestRepository.existsByStudentUserAndListingAndStatusIn(student, listing, ACTIVE_REQUEST_STATUSES.stream().toList())) {
            throw new BadRequestException("You have already requested or booked this room.");
        }
        
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setListing(listing);
        bookingRequest.setStudentUser(student);
        bookingRequest.setOwnerUser(listing.getOwnerUser());
        bookingRequest.setRequestedRent(listing.getRentAmount());
        bookingRequest.setMessage(request == null ? null : request.message());
        bookingRequest.setStatus(BookingRequestStatus.PENDING);
        BookingRequestResponse response = map(bookingRequestRepository.save(bookingRequest));
        publishBookingEvent("booking_request_created", response, listing.getOwnerUser(), student);
        return response;
    }

    @Transactional(readOnly = true)
    public List<BookingRequestResponse> studentRequests(UUID studentId) {
        return bookingRequestRepository.findByStudentUserOrderByRequestedAtDesc(currentUserService.requireUser(studentId))
                .stream()
                .filter(req -> req.getStatus() == BookingRequestStatus.PENDING)
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingRequestResponse> ownerRequests(UUID ownerId) {
        return bookingRequestRepository.findByOwnerUserOrderByRequestedAtDesc(currentUserService.requireUser(ownerId))
                .stream()
                .filter(req -> req.getStatus() == BookingRequestStatus.PENDING)
                .map(this::map)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"listing-search", "listing-detail"}, allEntries = true)
    public BookingRequestResponse acceptRequest(UUID ownerId, UUID bookingRequestId) {
        User owner = currentUserService.requireUser(ownerId);
        BookingRequest bookingRequest = bookingRequestRepository.findByIdAndOwnerUserWithLock(bookingRequestId, owner)
                .orElseThrow(() -> new NotFoundException("Booking request not found."));
        if (bookingRequest.getStatus() != BookingRequestStatus.PENDING) {
            throw new BadRequestException("This booking request has already been processed.");
        }
        bookingRequest.setStatus(BookingRequestStatus.ACCEPTED);
        bookingRequest.setDecidedAt(OffsetDateTime.now());
        bookingRequest.setDecidedBy(owner);
        bookingRequest = bookingRequestRepository.save(bookingRequest);

        Listing listing = bookingRequest.getListing();
        listing.setAvailableCapacity(Math.max(0, listing.getAvailableCapacity() - 1));

        LocalDate acceptanceDate = LocalDate.now();
        LocalDate billingCycleStart = acceptanceDate;
        LocalDate billingCycleEnd = acceptanceDate.plusMonths(1).minusDays(1);
        LocalDate nextDueDate = acceptanceDate.plusMonths(1);

        ActiveStay activeStay = new ActiveStay();
        activeStay.setBookingRequest(bookingRequest);
        activeStay.setListing(listing);
        activeStay.setStudentUser(bookingRequest.getStudentUser());
        activeStay.setOwnerUser(owner);
        activeStay.setRoomCode("RM-" + bookingRequest.getId().toString().substring(0, 8).toUpperCase());
        activeStay.setJoinDate(acceptanceDate);
        activeStay.setCurrentMonthStart(billingCycleStart);
        activeStay.setCurrentMonthEnd(billingCycleEnd);
        activeStay.setMonthlyRent(bookingRequest.getRequestedRent());
        activeStay.setStatus(ActiveStayStatus.ACTIVE);
        activeStay.setCurrentPaymentStatus(PaymentStatus.UNPAID);
        activeStay.setNextDueDate(nextDueDate);
        activeStay = activeStayRepository.save(activeStay);

        RentPayment payment = new RentPayment();
        payment.setActiveStay(activeStay);
        payment.setStudentUser(activeStay.getStudentUser());
        payment.setOwnerUser(activeStay.getOwnerUser());
        payment.setAmount(activeStay.getMonthlyRent());
        payment.setPeriodStart(billingCycleStart);
        payment.setPeriodEnd(billingCycleEnd);
        payment.setDueDate(nextDueDate);
        payment.setStatus(PaymentStatus.UNPAID);
        payment = rentPaymentRepository.save(payment);
        notificationService.notifyUser(
                bookingRequest.getStudentUser(),
                NotificationType.SUCCESS,
                "Booking accepted",
                "Your booking request for " + bookingRequest.getListing().getTitle() + " has been accepted.",
                "/student/bookings",
                null
        );
        BookingRequestResponse response = map(bookingRequest);
        publishBookingEvent("booking_request_accepted", response, owner, bookingRequest.getStudentUser());
        publishStayEvent("active_stay_created", map(activeStay), owner, bookingRequest.getStudentUser());
        publishPaymentEvent("rent_payment_created", map(payment), owner, bookingRequest.getStudentUser());
        return response;
    }

    @Transactional
    public BookingRequestResponse rejectRequest(UUID ownerId, UUID bookingRequestId, BookingReviewRequest request) {
        User owner = currentUserService.requireUser(ownerId);
        BookingRequest bookingRequest = bookingRequestRepository.findByIdAndOwnerUserWithLock(bookingRequestId, owner)
                .orElseThrow(() -> new NotFoundException("Booking request not found."));
        if (bookingRequest.getStatus() != BookingRequestStatus.PENDING) {
            throw new BadRequestException("This booking request has already been processed.");
        }
        bookingRequest.setStatus(BookingRequestStatus.REJECTED);
        bookingRequest.setRejectionReason(request == null ? null : request.reason());
        bookingRequest.setDecidedAt(OffsetDateTime.now());
        bookingRequest.setDecidedBy(owner);
        notificationService.notifyUser(
                bookingRequest.getStudentUser(),
                NotificationType.WARNING,
                "Booking rejected",
                request != null && request.reason() != null && !request.reason().isBlank()
                        ? request.reason()
                        : "Your booking request for " + bookingRequest.getListing().getTitle() + " was rejected.",
                "/student/bookings",
                null
        );
        BookingRequestResponse response = map(bookingRequestRepository.save(bookingRequest));
        publishBookingEvent("booking_request_rejected", response, owner, bookingRequest.getStudentUser());
        return response;
    }

    @Transactional
    public BookingRequestResponse revokeRequest(UUID studentId, UUID bookingRequestId) {
        User student = currentUserService.requireUser(studentId);
        BookingRequest bookingRequest = bookingRequestRepository.findByIdAndStudentUser(bookingRequestId, student)
                .orElseThrow(() -> new NotFoundException("Booking request not found."));
        bookingRequest.setStatus(BookingRequestStatus.REVOKED);
        bookingRequest = bookingRequestRepository.save(bookingRequest);
        BookingRequestResponse response = map(bookingRequest);
        publishBookingEvent("booking_request_revoked", response, bookingRequest.getOwnerUser(), student);
        return response;
    }

    @Transactional(readOnly = true)
    public ActiveStayResponse currentStay(UUID studentId) {
        User student = currentUserService.requireUser(studentId);
        ActiveStay stay = activeStayRepository.findTopByStudentUserAndStatusOrderByCreatedAtDesc(student, ActiveStayStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("No active stay found."));
        return map(stay);
    }

    @Transactional(readOnly = true)
    public List<ActiveStayResponse> ownerStays(UUID ownerId) {
        User owner = currentUserService.requireUser(ownerId);
        return activeStayRepository.findByOwnerUserAndStatusOrderByCreatedAtDesc(owner, ActiveStayStatus.ACTIVE)
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<RentPaymentResponse> payments(UUID userId) {
        User user = currentUserService.requireUser(userId);
        List<RentPayment> payments = user.getPrimaryRoleCode() == RoleName.OWNER
                ? rentPaymentRepository.findByOwnerUserOrderByPeriodStartDesc(user)
                : rentPaymentRepository.findByStudentUserOrderByPeriodStartDesc(user);
        return payments.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveBookingForListing(UUID studentId, UUID listingId) {
        User student = currentUserService.requireUser(studentId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        return bookingRequestRepository.existsByStudentUserAndListingAndStatusIn(
                student,
                listing,
                ACTIVE_REQUEST_STATUSES.stream().toList()
        );
    }

    @Transactional
    public RentPaymentResponse updatePayment(UUID ownerId, UUID paymentId, RentPaymentUpdateRequest request) {
        User owner = currentUserService.requireUser(ownerId);
        RentPayment payment = rentPaymentRepository.findByIdAndOwnerUser(paymentId, owner)
                .orElseThrow(() -> new NotFoundException("Payment not found."));
        if (request.status() != null) {
            payment.setStatus(request.status());
            if (request.status() == PaymentStatus.PAID) {
                payment.setPaidAt(OffsetDateTime.now());
                payment.getActiveStay().setCurrentPaymentStatus(PaymentStatus.PAID);
                payment.setReminderMessage(null);
                payment.getActiveStay().setReminderMessage(null);
            } else {
                payment.setPaidAt(null);
                payment.getActiveStay().setCurrentPaymentStatus(PaymentStatus.UNPAID);
            }
        }
        String reminderMessage = request.status() == PaymentStatus.PAID ? null : request.ownerReminder();
        payment.setReminderMessage(reminderMessage);
        payment.getActiveStay().setReminderMessage(reminderMessage);
        payment.setNotes(null);
        RentPayment savedPayment = rentPaymentRepository.save(payment);
        RentPaymentResponse response = map(savedPayment);
        publishPaymentEvent("rent_payment_updated", response, owner, savedPayment.getStudentUser());
        publishStayEvent("active_stay_updated", map(savedPayment.getActiveStay()), owner, savedPayment.getStudentUser());
        return response;
    }

    @Transactional
    public StayCancelResponse createCancelRequest(UUID studentId, UUID activeStayId, CancelRequestCreateRequest request) {
        User student = currentUserService.requireUser(studentId);
        ActiveStay activeStay = activeStayRepository.findById(activeStayId)
                .orElseThrow(() -> new NotFoundException("Active stay not found."));
        if (!activeStay.getStudentUser().getId().equals(student.getId())) {
            throw new BadRequestException("You can only cancel your own active stay.");
        }
        if (activeStay.getStatus() != ActiveStayStatus.ACTIVE) {
            throw new BadRequestException("This stay is no longer active.");
        }
        StayCancelRequest cancelRequest = new StayCancelRequest();
        cancelRequest.setActiveStay(activeStay);
        cancelRequest.setStudentUser(student);
        cancelRequest.setOwnerUser(activeStay.getOwnerUser());
        cancelRequest.setReason(request.reason());
        cancelRequest.setAccountStatusSnapshot(activeStay.getCurrentPaymentStatus().name());
        cancelRequest.setStatus(CancelRequestStatus.UNDER_PROGRESS);
        StayCancelRequest savedRequest = stayCancelRequestRepository.save(cancelRequest);
        StayCancelResponse response = map(savedRequest);
        publishCancelEvent("cancel_request_created", response, activeStay.getOwnerUser(), student);
        return response;
    }

    @Transactional(readOnly = true)
    public List<StayCancelResponse> myCancelRequests(UUID userId) {
        User user = currentUserService.requireUser(userId);
        List<StayCancelRequest> cancelRequests = user.getPrimaryRoleCode() == RoleName.OWNER
                ? stayCancelRequestRepository.findByOwnerUserOrderByRequestedAtDesc(user)
                : stayCancelRequestRepository.findByStudentUserOrderByRequestedAtDesc(user);
        return cancelRequests.stream().map(this::map).toList();
    }

    @Transactional
    public StayCancelResponse reviewCancelRequest(UUID ownerId, UUID cancelRequestId, CancelRequestReviewRequest request) {
        User owner = currentUserService.requireUser(ownerId);
        StayCancelRequest cancelRequest = stayCancelRequestRepository.findByIdAndOwnerUserWithLock(cancelRequestId, owner)
                .orElseThrow(() -> new NotFoundException("Cancel request not found."));
        if (cancelRequest.getStatus() != CancelRequestStatus.UNDER_PROGRESS) {
            throw new BadRequestException("This cancel request has already been processed.");
        }
        cancelRequest.setOwnerReason(request.ownerReason());
        cancelRequest.setResolvedAt(OffsetDateTime.now());
        if (request.accept()) {
            cancelRequest.setStatus(CancelRequestStatus.ACCEPTED);
            cancelRequest.getActiveStay().setStatus(ActiveStayStatus.ENDED);
            cancelRequest.getActiveStay().setReminderMessage(null);
            cancelRequest.getActiveStay().setNextDueDate(null);
            cancelRequest.getActiveStay().getListing().setAvailableCapacity(cancelRequest.getActiveStay().getListing().getAvailableCapacity() + 1);
            
            User student = cancelRequest.getStudentUser();
            Listing listing = cancelRequest.getActiveStay().getListing();
            
            // Delete all payment records for this stay
            rentPaymentRepository.findByOwnerUserOrderByPeriodStartDesc(owner).stream()
                    .filter(payment -> payment.getActiveStay().getId().equals(cancelRequest.getActiveStay().getId()))
                    .forEach(rentPaymentRepository::delete);
            
            // Delete all complaints between owner and student (both directions)
            complaintRepository.deleteByComplainantUserAndAgainstUser(owner, student);
            complaintRepository.deleteByAgainstUserAndComplainantUser(owner, student);
            
            // Delete all feedback/reviews by this student for this listing
            feedbackRepository.findByUserAndListing(student, listing)
                    .forEach(feedbackRepository::delete);
            
            // Update the booking request status to CANCELLED to allow rebooking
            BookingRequest originalBookingRequest = cancelRequest.getActiveStay().getBookingRequest();
            if (originalBookingRequest != null) {
                originalBookingRequest.setStatus(BookingRequestStatus.CANCELLED);
                bookingRequestRepository.save(originalBookingRequest);
            }
            
            notificationService.notifyUser(
                    student,
                    NotificationType.SUCCESS,
                    "Cancel request accepted",
                    "Your room cancel request has been accepted.",
                    "/student/cancel-requests",
                    null
            );
        } else {
            cancelRequest.setStatus(CancelRequestStatus.REJECTED);
            notificationService.notifyUser(
                    cancelRequest.getStudentUser(),
                    NotificationType.WARNING,
                    "Cancel request rejected",
                    request.ownerReason() == null || request.ownerReason().isBlank()
                            ? "Your room cancel request has been rejected."
                            : request.ownerReason(),
                    "/student/cancel-requests",
                    null
            );
        }
        StayCancelRequest savedRequest = stayCancelRequestRepository.save(cancelRequest);
        StayCancelResponse response = map(savedRequest);
        publishCancelEvent("cancel_request_reviewed", response, owner, savedRequest.getStudentUser());
        publishStayEvent("active_stay_updated", map(savedRequest.getActiveStay()), owner, savedRequest.getStudentUser());
        return response;
    }

    @Transactional
    public void deleteActiveStay(UUID ownerId, UUID activeStayId) {
        User owner = currentUserService.requireUser(ownerId);
        ActiveStay activeStay = activeStayRepository.findById(activeStayId)
                .orElseThrow(() -> new NotFoundException("Active stay not found."));
        
        // Verify ownership
        if (!activeStay.getOwnerUser().getId().equals(ownerId)) {
            throw new BadRequestException("You do not have permission to delete this student connection.");
        }
        
        User student = activeStay.getStudentUser();
        Listing listing = activeStay.getListing();
        
        // Delete all complaints between owner and student (both directions)
        complaintRepository.deleteByComplainantUserAndAgainstUser(owner, student);
        complaintRepository.deleteByAgainstUserAndComplainantUser(owner, student);
        
        // Delete all payment records for this stay
        rentPaymentRepository.findByOwnerUserOrderByPeriodStartDesc(owner).stream()
                .filter(payment -> payment.getActiveStay().getId().equals(activeStayId))
                .forEach(rentPaymentRepository::delete);
        
        // Delete any cancel requests for this stay
        stayCancelRequestRepository.findByOwnerUserOrderByRequestedAtDesc(owner).stream()
                .filter(request -> request.getActiveStay().getId().equals(activeStayId))
                .forEach(stayCancelRequestRepository::delete);
        
        // Delete all feedback/reviews by this student for this listing
        feedbackRepository.findByUserAndListing(student, listing)
                .forEach(feedbackRepository::delete);
        
        // Update the booking request status to CANCELLED to allow rebooking
        BookingRequest originalBookingRequest = activeStay.getBookingRequest();
        if (originalBookingRequest != null) {
            originalBookingRequest.setStatus(BookingRequestStatus.CANCELLED);
            bookingRequestRepository.save(originalBookingRequest);
        }
        
        // Increase available capacity
        listing.setAvailableCapacity(listing.getAvailableCapacity() + 1);
        
        // Delete the active stay
        activeStayRepository.delete(activeStay);
        
        // Notify student
        notificationService.notifyUser(
                student,
                NotificationType.WARNING,
                "Connection removed",
                "Your stay connection has been removed by the owner.",
                "/student/dashboard",
                null
        );
        
        // Publish realtime events
        realtimeEventPublisher.publishUserEvent(owner.getId().toString(), "active_stay_deleted", Map.of("activeStayId", activeStayId));
        realtimeEventPublisher.publishUserEvent(student.getId().toString(), "active_stay_deleted", Map.of("activeStayId", activeStayId));
    }

    private void publishBookingEvent(String eventType, BookingRequestResponse payload, User owner, User student) {
        publishUserPairEvent(eventType, payload, owner, student);
        realtimeEventPublisher.publishRoleEvent("OWNER", eventType, payload);
        realtimeEventPublisher.publishRoleEvent("STUDENT", eventType, payload);
    }

    private void publishStayEvent(String eventType, ActiveStayResponse payload, User owner, User student) {
        publishUserPairEvent(eventType, payload, owner, student);
    }

    private void publishPaymentEvent(String eventType, RentPaymentResponse payload, User owner, User student) {
        publishUserPairEvent(eventType, payload, owner, student);
    }

    private void publishCancelEvent(String eventType, StayCancelResponse payload, User owner, User student) {
        publishUserPairEvent(eventType, payload, owner, student);
        realtimeEventPublisher.publishRoleEvent("OWNER", eventType, payload);
        realtimeEventPublisher.publishRoleEvent("STUDENT", eventType, payload);
    }

    private void publishUserPairEvent(String eventType, Object payload, User firstUser, User secondUser) {
        if (firstUser != null) {
            realtimeEventPublisher.publishUserEvent(firstUser.getId().toString(), eventType, payload);
        }
        if (secondUser != null) {
            realtimeEventPublisher.publishUserEvent(secondUser.getId().toString(), eventType, payload);
        }
    }

    private BookingRequestResponse map(BookingRequest bookingRequest) {
        StudentProfile studentProfile = studentProfileRepository.findByUser(bookingRequest.getStudentUser()).orElse(null);
        return new BookingRequestResponse(
                bookingRequest.getId(),
                bookingRequest.getListing().getId(),
                bookingRequest.getListing().getTitle(),
                bookingRequest.getListing().getLocality() != null ? bookingRequest.getListing().getLocality() : bookingRequest.getListing().getAddressLineOne(),
                bookingRequest.getOwnerUser().getUserCode(),
                bookingRequest.getOwnerUser().getDisplayName(),
                bookingRequest.getStudentUser().getUserCode(),
                bookingRequest.getStudentUser().getDisplayName(),
                studentProfile == null ? null : studentProfile.getCollegeName(),
                bookingRequest.getRequestedRent(),
                bookingRequest.getListing().getAvailableCapacity() == null ? 0 : bookingRequest.getListing().getAvailableCapacity(),
                bookingRequest.getListing().getTotalCapacity() == null ? 0 : bookingRequest.getListing().getTotalCapacity(),
                bookingRequest.getMessage(),
                bookingRequest.getStatus(),
                bookingRequest.getRejectionReason(),
                bookingRequest.getRequestedAt()
        );
    }

    private ActiveStayResponse map(ActiveStay activeStay) {
        StudentProfile studentProfile = studentProfileRepository.findByUser(activeStay.getStudentUser()).orElse(null);
        return new ActiveStayResponse(
                activeStay.getId(),
                activeStay.getListing().getId(),
                activeStay.getListing().getTitle(),
                activeStay.getListing().getLocality() != null ? activeStay.getListing().getLocality() : activeStay.getListing().getAddressLineOne(),
                activeStay.getListing().getRoomKind(),
                activeStay.getRoomCode(),
                activeStay.getOwnerUser().getUserCode(),
                activeStay.getOwnerUser().getDisplayName(),
                activeStay.getOwnerUser().getEmail(),
                activeStay.getOwnerUser().getMobileNumber(),
                activeStay.getStudentUser().getUserCode(),
                activeStay.getStudentUser().getDisplayName(),
                studentProfile == null ? null : studentProfile.getCollegeName(),
                studentProfile == null ? null : firstNonBlank(studentProfile.getEnrollmentNumber(), studentProfile.getPrn()),
                studentProfile == null ? null : studentProfile.getCurrentLocation(),
                activeStay.getStudentUser().getEmail(),
                activeStay.getStudentUser().getMobileNumber(),
                activeStay.getJoinDate(),
                activeStay.getMonthlyRent(),
                activeStay.getStatus(),
                activeStay.getCurrentPaymentStatus(),
                activeStay.getNextDueDate(),
                activeStay.getReminderMessage()
        );
    }

    private RentPaymentResponse map(RentPayment payment) {
        return new RentPaymentResponse(
                payment.getId(),
                payment.getActiveStay().getId(),
                payment.getActiveStay().getListing().getTitle(),
                payment.getActiveStay().getRoomCode(),
                payment.getOwnerUser().getUserCode(),
                payment.getOwnerUser().getDisplayName(),
                payment.getStudentUser().getUserCode(),
                payment.getStudentUser().getDisplayName(),
                payment.getAmount(),
                payment.getPeriodStart(),
                payment.getPeriodEnd(),
                payment.getDueDate(),
                payment.getPaidAt(),
                payment.getStatus(),
                payment.getReminderMessage(),
                payment.getNotes()
        );
    }

    private StayCancelResponse map(StayCancelRequest request) {
        return new StayCancelResponse(
                request.getId(),
                request.getActiveStay().getId(),
                request.getActiveStay().getListing().getTitle(),
                request.getActiveStay().getRoomCode(),
                request.getOwnerUser().getUserCode(),
                request.getOwnerUser().getDisplayName(),
                request.getStudentUser().getUserCode(),
                request.getStudentUser().getDisplayName(),
                request.getReason(),
                request.getAccountStatusSnapshot(),
                request.getStatus(),
                request.getOwnerReason(),
                request.getRequestedAt()
        );
    }

    private void ensureVerifiedAccess(User user) {
        if (!user.isProfileComplete() || !user.isIdentityVerified()) {
            throw new BadRequestException("Complete your profile and identity verification before using booking features.");
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
