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
import com.stazy.backend.common.enums.ActiveStayStatus;
import com.stazy.backend.common.enums.BookingRequestStatus;
import com.stazy.backend.common.enums.CancelRequestStatus;
import com.stazy.backend.common.enums.PaymentStatus;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.listing.repository.ListingRepository;
import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.profile.repository.StudentProfileRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.service.CurrentUserService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRequestRepository bookingRequestRepository;
    private final ActiveStayRepository activeStayRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final StayCancelRequestRepository stayCancelRequestRepository;
    private final ListingRepository listingRepository;
    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;

    public BookingService(
            BookingRequestRepository bookingRequestRepository,
            ActiveStayRepository activeStayRepository,
            RentPaymentRepository rentPaymentRepository,
            StayCancelRequestRepository stayCancelRequestRepository,
            ListingRepository listingRepository,
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.activeStayRepository = activeStayRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.stayCancelRequestRepository = stayCancelRequestRepository;
        this.listingRepository = listingRepository;
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional
    public BookingRequestResponse createRequest(UUID studentId, UUID listingId, BookingCreateRequest request) {
        User student = currentUserService.requireUser(studentId);
        if (student.getPrimaryRoleCode() != RoleName.STUDENT) {
            throw new BadRequestException("Only students can request bookings.");
        }
        ensureVerifiedAccess(student);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        if (listing.getAvailableCapacity() <= 0) {
            throw new BadRequestException("This listing is currently full.");
        }
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setListing(listing);
        bookingRequest.setStudentUser(student);
        bookingRequest.setOwnerUser(listing.getOwnerUser());
        bookingRequest.setRequestedRent(listing.getRentAmount());
        bookingRequest.setMessage(request == null ? null : request.message());
        bookingRequest.setStatus(BookingRequestStatus.PENDING);
        return map(bookingRequestRepository.save(bookingRequest));
    }

    @Transactional(readOnly = true)
    public List<BookingRequestResponse> studentRequests(UUID studentId) {
        return bookingRequestRepository.findByStudentUserOrderByRequestedAtDesc(currentUserService.requireUser(studentId))
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingRequestResponse> ownerRequests(UUID ownerId) {
        return bookingRequestRepository.findByOwnerUserOrderByRequestedAtDesc(currentUserService.requireUser(ownerId))
                .stream().map(this::map).toList();
    }

    @Transactional
    public BookingRequestResponse acceptRequest(UUID ownerId, UUID bookingRequestId) {
        User owner = currentUserService.requireUser(ownerId);
        BookingRequest bookingRequest = bookingRequestRepository.findByIdAndOwnerUser(bookingRequestId, owner)
                .orElseThrow(() -> new NotFoundException("Booking request not found."));
        bookingRequest.setStatus(BookingRequestStatus.ACCEPTED);
        bookingRequest.setDecidedAt(OffsetDateTime.now());
        bookingRequest.setDecidedBy(owner);
        bookingRequest = bookingRequestRepository.save(bookingRequest);

        Listing listing = bookingRequest.getListing();
        listing.setAvailableCapacity(Math.max(0, listing.getAvailableCapacity() - 1));

        ActiveStay activeStay = new ActiveStay();
        activeStay.setBookingRequest(bookingRequest);
        activeStay.setListing(listing);
        activeStay.setStudentUser(bookingRequest.getStudentUser());
        activeStay.setOwnerUser(owner);
        activeStay.setRoomCode("RM-" + bookingRequest.getId().toString().substring(0, 8).toUpperCase());
        activeStay.setJoinDate(LocalDate.now());
        activeStay.setCurrentMonthStart(LocalDate.now().withDayOfMonth(1));
        activeStay.setCurrentMonthEnd(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        activeStay.setMonthlyRent(bookingRequest.getRequestedRent());
        activeStay.setStatus(ActiveStayStatus.ACTIVE);
        activeStay.setCurrentPaymentStatus(PaymentStatus.UNPAID);
        activeStay.setNextDueDate(LocalDate.now().plusMonths(1).withDayOfMonth(1));
        activeStayRepository.save(activeStay);

        RentPayment payment = new RentPayment();
        payment.setActiveStay(activeStay);
        payment.setStudentUser(activeStay.getStudentUser());
        payment.setOwnerUser(activeStay.getOwnerUser());
        payment.setAmount(activeStay.getMonthlyRent());
        payment.setPeriodStart(activeStay.getCurrentMonthStart());
        payment.setPeriodEnd(activeStay.getCurrentMonthEnd());
        payment.setDueDate(activeStay.getNextDueDate());
        payment.setStatus(PaymentStatus.UNPAID);
        rentPaymentRepository.save(payment);
        return map(bookingRequest);
    }

    @Transactional
    public BookingRequestResponse rejectRequest(UUID ownerId, UUID bookingRequestId, BookingReviewRequest request) {
        User owner = currentUserService.requireUser(ownerId);
        BookingRequest bookingRequest = bookingRequestRepository.findByIdAndOwnerUser(bookingRequestId, owner)
                .orElseThrow(() -> new NotFoundException("Booking request not found."));
        bookingRequest.setStatus(BookingRequestStatus.REJECTED);
        bookingRequest.setRejectionReason(request == null ? null : request.reason());
        bookingRequest.setDecidedAt(OffsetDateTime.now());
        bookingRequest.setDecidedBy(owner);
        return map(bookingRequestRepository.save(bookingRequest));
    }

    @Transactional
    public BookingRequestResponse revokeRequest(UUID studentId, UUID bookingRequestId) {
        User student = currentUserService.requireUser(studentId);
        BookingRequest bookingRequest = bookingRequestRepository.findByIdAndStudentUser(bookingRequestId, student)
                .orElseThrow(() -> new NotFoundException("Booking request not found."));
        bookingRequest.setStatus(BookingRequestStatus.REVOKED);
        return map(bookingRequestRepository.save(bookingRequest));
    }

    @Transactional(readOnly = true)
    public ActiveStayResponse currentStay(UUID studentId) {
        ActiveStay stay = activeStayRepository.findTopByStudentUserOrderByCreatedAtDesc(currentUserService.requireUser(studentId))
                .orElseThrow(() -> new NotFoundException("No active stay found."));
        return map(stay);
    }

    @Transactional(readOnly = true)
    public List<ActiveStayResponse> ownerStays(UUID ownerId) {
        return activeStayRepository.findByOwnerUserOrderByCreatedAtDesc(currentUserService.requireUser(ownerId))
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
            }
        }
        if (request.dueDate() != null) {
            payment.setDueDate(request.dueDate());
            payment.getActiveStay().setNextDueDate(request.dueDate());
        }
        payment.setReminderMessage(request.reminderMessage());
        payment.setNotes(request.notes());
        return map(rentPaymentRepository.save(payment));
    }

    @Transactional
    public StayCancelResponse createCancelRequest(UUID studentId, UUID activeStayId, CancelRequestCreateRequest request) {
        User student = currentUserService.requireUser(studentId);
        ActiveStay activeStay = activeStayRepository.findById(activeStayId)
                .orElseThrow(() -> new NotFoundException("Active stay not found."));
        StayCancelRequest cancelRequest = new StayCancelRequest();
        cancelRequest.setActiveStay(activeStay);
        cancelRequest.setStudentUser(student);
        cancelRequest.setOwnerUser(activeStay.getOwnerUser());
        cancelRequest.setReason(request.reason());
        cancelRequest.setAccountStatusSnapshot(activeStay.getCurrentPaymentStatus().name());
        cancelRequest.setStatus(CancelRequestStatus.UNDER_PROGRESS);
        return map(stayCancelRequestRepository.save(cancelRequest));
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
        StayCancelRequest cancelRequest = stayCancelRequestRepository.findByIdAndOwnerUser(cancelRequestId, owner)
                .orElseThrow(() -> new NotFoundException("Cancel request not found."));
        cancelRequest.setOwnerReason(request.ownerReason());
        cancelRequest.setResolvedAt(OffsetDateTime.now());
        if (request.accept()) {
            cancelRequest.setStatus(CancelRequestStatus.ACCEPTED);
            cancelRequest.getActiveStay().setStatus(ActiveStayStatus.ENDED);
            cancelRequest.getActiveStay().getListing().setAvailableCapacity(cancelRequest.getActiveStay().getListing().getAvailableCapacity() + 1);
        } else {
            cancelRequest.setStatus(CancelRequestStatus.REJECTED);
        }
        return map(stayCancelRequestRepository.save(cancelRequest));
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
