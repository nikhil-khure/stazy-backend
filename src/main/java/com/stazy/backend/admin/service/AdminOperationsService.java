package com.stazy.backend.admin.service;

import com.stazy.backend.admin.dto.AdminHiringRequestCreate;
import com.stazy.backend.admin.dto.AdminHiringRequestResponse;
import com.stazy.backend.admin.dto.AdminQueryCreateRequest;
import com.stazy.backend.admin.dto.AdminQueryReplyRequest;
import com.stazy.backend.admin.dto.AdminQueryResponse;
import com.stazy.backend.admin.dto.ContactMessageRequest;
import com.stazy.backend.admin.dto.FeedbackResponse;
import com.stazy.backend.admin.dto.FeedbackSubmissionRequest;
import com.stazy.backend.admin.dto.HireAdminRequest;
import com.stazy.backend.admin.dto.PublicFeedbackRequest;
import com.stazy.backend.admin.entity.AdminHiringRequest;
import com.stazy.backend.admin.entity.AdminQuery;
import com.stazy.backend.admin.entity.ContactMessage;
import com.stazy.backend.admin.entity.Feedback;
import com.stazy.backend.admin.repository.AdminHiringRequestRepository;
import com.stazy.backend.admin.repository.AdminQueryRepository;
import com.stazy.backend.admin.repository.ContactMessageRepository;
import com.stazy.backend.admin.repository.FeedbackRepository;
import com.stazy.backend.common.enums.AdminQueryStatus;
import com.stazy.backend.common.enums.AuthProvider;
import com.stazy.backend.common.enums.FeedbackScope;
import com.stazy.backend.common.enums.FeedbackVisibilityStatus;
import com.stazy.backend.common.enums.HiringRequestStatus;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.common.util.PasswordRules;
import com.stazy.backend.integration.cloudinary.CloudinaryService;
import com.stazy.backend.integration.cloudinary.DownloadedAsset;
import com.stazy.backend.integration.cloudinary.UploadedAsset;
import com.stazy.backend.profile.entity.AdminProfile;
import com.stazy.backend.profile.entity.City;
import com.stazy.backend.profile.repository.AdminProfileRepository;
import com.stazy.backend.profile.repository.CityRepository;
import com.stazy.backend.user.entity.Role;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.entity.UserRole;
import com.stazy.backend.user.repository.RoleRepository;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOperationsService {

    private final ContactMessageRepository contactMessageRepository;
    private final FeedbackRepository feedbackRepository;
    private final AdminHiringRequestRepository adminHiringRequestRepository;
    private final AdminQueryRepository adminQueryRepository;
    private final CurrentUserService currentUserService;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final CityRepository cityRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminOperationsService(
            ContactMessageRepository contactMessageRepository,
            FeedbackRepository feedbackRepository,
            AdminHiringRequestRepository adminHiringRequestRepository,
            AdminQueryRepository adminQueryRepository,
            CurrentUserService currentUserService,
            CloudinaryService cloudinaryService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            AdminProfileRepository adminProfileRepository,
            CityRepository cityRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.contactMessageRepository = contactMessageRepository;
        this.feedbackRepository = feedbackRepository;
        this.adminHiringRequestRepository = adminHiringRequestRepository;
        this.adminQueryRepository = adminQueryRepository;
        this.currentUserService = currentUserService;
        this.cloudinaryService = cloudinaryService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.cityRepository = cityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void submitContact(ContactMessageRequest request) {
        ContactMessage message = new ContactMessage();
        message.setFullName(request.fullName().trim());
        message.setEmail(request.email().trim().toLowerCase());
        message.setMessage(request.message().trim());
        contactMessageRepository.save(message);
    }

    @Transactional
    public void submitPublicFeedback(PublicFeedbackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setFeedbackScope(FeedbackScope.PLATFORM);
        feedback.setMessage(request.message().trim());
        feedback.setDisplayNameSnapshot(request.fullName().trim());
        feedback.setEmailSnapshot(request.email().trim().toLowerCase());
        feedback.setAuthenticated(false);
        feedback.setPublished(false);
        feedback.setVisibilityStatus(FeedbackVisibilityStatus.PENDING_REVIEW);
        feedbackRepository.save(feedback);
    }

    @Transactional
    public FeedbackResponse submitAuthenticatedFeedback(UUID userId, FeedbackSubmissionRequest request) {
        User user = currentUserService.requireUser(userId);
        User targetUser = null;
        if (request.targetUserCode() != null && !request.targetUserCode().isBlank()) {
            targetUser = userRepository.findByUserCodeIgnoreCase(request.targetUserCode().trim())
                    .orElseThrow(() -> new NotFoundException("Target user not found."));
        }
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setTargetUser(targetUser);
        feedback.setFeedbackScope(request.feedbackScope());
        feedback.setRating(request.rating());
        feedback.setMessage(request.message().trim());
        feedback.setDisplayNameSnapshot(user.getDisplayName());
        feedback.setEmailSnapshot(user.getEmail());
        feedback.setLocationSnapshot(request.location());
        feedback.setAuthenticated(true);
        feedback.setPublished(false);
        feedback.setVisibilityStatus(FeedbackVisibilityStatus.PENDING_REVIEW);
        return map(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getPublishedFeedback() {
        return feedbackRepository.findByIsPublishedTrueOrderByCreatedAtDesc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackByAuth(boolean authenticated) {
        return feedbackRepository.findByIsAuthenticatedOrderByCreatedAtDesc(authenticated).stream().map(this::map).toList();
    }

    @Transactional
    public FeedbackResponse publishFeedback(UUID reviewerId, UUID feedbackId) {
        requireSuperAdmin(reviewerId);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new NotFoundException("Feedback not found."));
        feedback.setPublished(true);
        feedback.setVisibilityStatus(FeedbackVisibilityStatus.PUBLISHED);
        return map(feedbackRepository.save(feedback));
    }

    @Transactional
    public void deleteFeedback(UUID reviewerId, UUID feedbackId) {
        requireSuperAdmin(reviewerId);
        feedbackRepository.deleteById(feedbackId);
    }

    @Transactional
    public AdminHiringRequestResponse submitHiringRequest(AdminHiringRequestCreate request) {
        if (request.getResume() == null || request.getResume().isEmpty()) {
            throw new BadRequestException("Resume is required.");
        }
        UploadedAsset uploadedAsset = cloudinaryService.uploadRaw(request.getResume(), "stazy/admin-hiring");
        AdminHiringRequest hiringRequest = new AdminHiringRequest();
        hiringRequest.setFullName(request.getFullName().trim());
        hiringRequest.setMobileNumber(request.getMobileNumber().trim());
        hiringRequest.setEmail(request.getEmail().trim().toLowerCase());
        hiringRequest.setResumeUrl(uploadedAsset.url());
        hiringRequest.setResumePublicId(uploadedAsset.publicId());
        return map(adminHiringRequestRepository.save(hiringRequest));
    }

    @Transactional(readOnly = true)
    public List<AdminHiringRequestResponse> getHiringRequests(HiringRequestStatus status) {
        return adminHiringRequestRepository.findByStatusOrderByCreatedAtDesc(status).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public DownloadedAsset getHiringRequestResume(UUID reviewerId, UUID hiringRequestId) {
        requireSuperAdmin(reviewerId);
        AdminHiringRequest hiringRequest = findHiringRequest(hiringRequestId);
        if (hiringRequest.getResumeUrl() == null || hiringRequest.getResumeUrl().isBlank()) {
            throw new BadRequestException("Resume file is not available.");
        }
        return cloudinaryService.downloadFile(hiringRequest.getResumePublicId(), hiringRequest.getResumeUrl());
    }

    @Transactional
    public AdminHiringRequestResponse hireAdmin(UUID reviewerId, UUID hiringRequestId, HireAdminRequest request) {
        requireSuperAdmin(reviewerId);
        PasswordRules.validate(request.password());
        if (userRepository.existsByUserCodeIgnoreCase(request.adminId())) {
            throw new BadRequestException("This admin ID is already assigned.");
        }
        if (userRepository.existsByEmailIgnoreCase(findHiringRequest(hiringRequestId).getEmail())) {
            throw new BadRequestException("An account with this email already exists.");
        }
        AdminHiringRequest hiringRequest = findHiringRequest(hiringRequestId);
        if (hiringRequest.getStatus() != HiringRequestStatus.PENDING) {
            throw new BadRequestException("This hiring request has already been reviewed.");
        }

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new NotFoundException("City not found."));
        Role role = roleRepository.findByCode(RoleName.ADMIN)
                .orElseThrow(() -> new NotFoundException("ADMIN role is not configured."));

        User user = new User();
        user.setUserCode(request.adminId().trim().toUpperCase());
        user.setPrimaryRoleCode(RoleName.ADMIN);
        user.setDisplayName(hiringRequest.getFullName());
        user.setEmail(hiringRequest.getEmail());
        user.setMobileNumber(hiringRequest.getMobileNumber());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider(AuthProvider.LOCAL);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);

        user = userRepository.save(user);

        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setUser(user);
        adminProfile.setCity(city);
        adminProfile.setSecretCodeHash(passwordEncoder.encode(request.secretCode()));
        adminProfileRepository.save(adminProfile);

        hiringRequest.setStatus(HiringRequestStatus.ACCEPTED);
        hiringRequest.setAssignedCity(city);
        hiringRequest.setReviewedBy(currentUserService.requireUser(reviewerId));
        hiringRequest.setReviewedAt(OffsetDateTime.now());
        hiringRequest.setReviewNotes(request.reviewNotes());
        return map(adminHiringRequestRepository.save(hiringRequest));
    }

    @Transactional
    public AdminHiringRequestResponse rejectHiringRequest(UUID reviewerId, UUID hiringRequestId, String reviewNotes) {
        requireSuperAdmin(reviewerId);
        AdminHiringRequest hiringRequest = findHiringRequest(hiringRequestId);
        hiringRequest.setStatus(HiringRequestStatus.REJECTED);
        hiringRequest.setReviewedBy(currentUserService.requireUser(reviewerId));
        hiringRequest.setReviewedAt(OffsetDateTime.now());
        hiringRequest.setReviewNotes(reviewNotes);
        return map(adminHiringRequestRepository.save(hiringRequest));
    }

    @Transactional
    public AdminQueryResponse createAdminQuery(UUID adminUserId, AdminQueryCreateRequest request) {
        User adminUser = currentUserService.requireUser(adminUserId);
        if (adminUser.getPrimaryRoleCode() != RoleName.ADMIN) {
            throw new BadRequestException("Only admins can submit admin queries.");
        }
        AdminQuery query = new AdminQuery();
        query.setAdminUser(adminUser);
        query.setSubject(request.subject());
        query.setMessage(request.message());
        return map(adminQueryRepository.save(query));
    }

    @Transactional(readOnly = true)
    public List<AdminQueryResponse> getAdminQueries(UUID adminUserId) {
        User adminUser = currentUserService.requireUser(adminUserId);
        return adminQueryRepository.findByAdminUserOrderByCreatedAtDesc(adminUser).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminQueryResponse> getAllQueries(UUID reviewerId) {
        requireSuperAdmin(reviewerId);
        return adminQueryRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::map)
                .toList();
    }

    @Transactional
    public AdminQueryResponse replyToQuery(UUID reviewerId, UUID queryId, AdminQueryReplyRequest request) {
        requireSuperAdmin(reviewerId);
        AdminQuery query = adminQueryRepository.findById(queryId)
                .orElseThrow(() -> new NotFoundException("Admin query not found."));
        query.setReplyMessage(request.replyMessage());
        query.setStatus(AdminQueryStatus.REPLIED);
        query.setRepliedByUser(currentUserService.requireUser(reviewerId));
        query.setRepliedAt(OffsetDateTime.now());
        return map(adminQueryRepository.save(query));
    }

    private AdminHiringRequest findHiringRequest(UUID hiringRequestId) {
        return adminHiringRequestRepository.findById(hiringRequestId)
                .orElseThrow(() -> new NotFoundException("Hiring request not found."));
    }

    private void requireSuperAdmin(UUID reviewerId) {
        User reviewer = currentUserService.requireUser(reviewerId);
        if (reviewer.getPrimaryRoleCode() != RoleName.SUPER_ADMIN) {
            throw new BadRequestException("Only super admins can perform this action.");
        }
    }

    private FeedbackResponse map(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getFeedbackScope(),
                feedback.getRating(),
                feedback.getMessage(),
                feedback.getDisplayNameSnapshot(),
                feedback.getEmailSnapshot(),
                feedback.getLocationSnapshot(),
                feedback.isAuthenticated(),
                feedback.isPublished(),
                feedback.getVisibilityStatus(),
                feedback.getCreatedAt()
        );
    }

    private AdminHiringRequestResponse map(AdminHiringRequest request) {
        return new AdminHiringRequestResponse(
                request.getId(),
                request.getFullName(),
                request.getMobileNumber(),
                request.getEmail(),
                request.getResumeUrl(),
                request.getStatus(),
                request.getReviewNotes(),
                request.getCreatedAt()
        );
    }

    private AdminQueryResponse map(AdminQuery query) {
        return new AdminQueryResponse(
                query.getId(),
                query.getAdminUser().getUserCode(),
                query.getSubject(),
                query.getMessage(),
                query.getStatus(),
                query.getReplyMessage(),
                query.getCreatedAt(),
                query.getRepliedAt()
        );
    }
}
