package com.stazy.backend.admin.service;

import com.stazy.backend.admin.dto.AdminDashboardStatsResponse;
import com.stazy.backend.admin.dto.CityCreateRequest;
import com.stazy.backend.admin.dto.CityAnalyticsResponse;
import com.stazy.backend.admin.dto.ConnectedAdminResponse;
import com.stazy.backend.admin.dto.ManagedOwnerResponse;
import com.stazy.backend.admin.dto.ManagedStudentResponse;
import com.stazy.backend.admin.dto.ModeratedUserResponse;
import com.stazy.backend.admin.dto.PendingListingResponse;
import com.stazy.backend.admin.dto.PlatformStatsResponse;
import com.stazy.backend.booking.entity.ActiveStay;
import com.stazy.backend.common.events.RealtimeEventPublisher;
import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.enums.ComplaintStatus;
import com.stazy.backend.common.enums.EmployeeStatus;
import com.stazy.backend.common.enums.ListingStatus;
import com.stazy.backend.common.enums.MediaType;
import com.stazy.backend.common.enums.NotificationType;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.complaint.repository.ComplaintRepository;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.listing.entity.ListingMedia;
import com.stazy.backend.listing.repository.ListingMediaRepository;
import com.stazy.backend.listing.repository.ListingRepository;
import com.stazy.backend.profile.entity.AdminProfile;
import com.stazy.backend.profile.entity.City;
import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.profile.repository.AdminProfileRepository;
import com.stazy.backend.profile.repository.CityRepository;
import com.stazy.backend.profile.repository.OwnerProfileRepository;
import com.stazy.backend.profile.repository.StudentProfileRepository;
import com.stazy.backend.notification.service.NotificationService;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminModerationService {

    private static final List<ComplaintStatus> ACTIVE_COMPLAINT_STATUSES = List.of(
            ComplaintStatus.OPEN,
            ComplaintStatus.UNDER_PROGRESS,
            ComplaintStatus.AWAITING_JUSTIFICATION
    );

    private static final List<ComplaintStatus> RESOLVED_COMPLAINT_STATUSES = List.of(
            ComplaintStatus.RESOLVED,
            ComplaintStatus.CLOSED
    );

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ListingRepository listingRepository;
    private final ListingMediaRepository listingMediaRepository;
    private final ComplaintRepository complaintRepository;
    private final CityRepository cityRepository;
    private final NotificationService notificationService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public AdminModerationService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            OwnerProfileRepository ownerProfileRepository,
            AdminProfileRepository adminProfileRepository,
            ListingRepository listingRepository,
            ListingMediaRepository listingMediaRepository,
            ComplaintRepository complaintRepository,
            CityRepository cityRepository,
            NotificationService notificationService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.listingRepository = listingRepository;
        this.listingMediaRepository = listingMediaRepository;
        this.complaintRepository = complaintRepository;
        this.cityRepository = cityRepository;
        this.notificationService = notificationService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getAdminDashboardStats(UUID reviewerId) {
        AdminScope scope = resolveAdminScope(reviewerId);
        long totalStudents = loadScopedStudents(scope).size();
        long totalOwners = loadScopedOwners(scope).size();
        long liveListings = loadScopedListings(scope).stream()
                .filter(listing -> listing.getStatus() == ListingStatus.LIVE)
                .count();
        long pendingReviewListings = loadScopedListings(scope).stream()
                .filter(listing -> listing.getStatus() == ListingStatus.UNDER_REVIEW)
                .count();
        return new AdminDashboardStatsResponse(totalStudents, totalOwners, liveListings, pendingReviewListings, scope.cityName());
    }

    @Transactional(readOnly = true)
    public List<ManagedStudentResponse> getManagedStudents(UUID reviewerId) {
        AdminScope scope = resolveAdminScope(reviewerId);
        return loadScopedStudents(scope).stream()
                .map(this::mapStudent)
                .sorted(Comparator.comparing(ManagedStudentResponse::displayName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagedOwnerResponse> getManagedOwners(UUID reviewerId) {
        AdminScope scope = resolveAdminScope(reviewerId);
        return loadScopedOwners(scope).stream()
                .map(this::mapOwner)
                .sorted(Comparator.comparing(ManagedOwnerResponse::displayName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingListingResponse> getPendingListings(UUID reviewerId) {
        AdminScope scope = resolveAdminScope(reviewerId);
        return loadScopedListings(scope).stream()
                .filter(listing -> listing.getStatus() == ListingStatus.UNDER_REVIEW)
                .map(this::mapPendingListing)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"listing-search", "listing-detail"}, allEntries = true)
    public PendingListingResponse goLive(UUID reviewerId, UUID listingId) {
        Listing listing = requireListingForAdminScope(reviewerId, listingId);
        listing.setStatus(ListingStatus.LIVE);
        listing.setPublishedAt(OffsetDateTime.now());
        listing.setRejectionReason(null);
        Listing savedListing = listingRepository.save(listing);
        PendingListingResponse response = mapPendingListing(savedListing);
        publishListingModerationEvent("listing_go_live", savedListing, response);
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = {"listing-search", "listing-detail"}, allEntries = true)
    public PendingListingResponse rejectListing(UUID reviewerId, UUID listingId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Rejection reason is required.");
        }
        Listing listing = requireListingForAdminScope(reviewerId, listingId);
        listing.setStatus(ListingStatus.REJECTED);
        listing.setRejectionReason(reason.trim());
        listing.setPublishedAt(null);
        Listing savedListing = listingRepository.save(listing);
        PendingListingResponse response = mapPendingListing(savedListing);
        publishListingModerationEvent("listing_rejected", savedListing, response);
        return response;
    }

    @Transactional
    public ModeratedUserResponse updateUserStatus(UUID reviewerId, UUID userId, AccountStatus status, String message) {
        User reviewer = requireAdminOrSuperAdmin(reviewerId);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        if (reviewer.getPrimaryRoleCode() == RoleName.ADMIN && target.getPrimaryRoleCode() != RoleName.STUDENT && target.getPrimaryRoleCode() != RoleName.OWNER) {
            throw new BadRequestException("Admins can only moderate student and owner accounts.");
        }
        if (target.getPrimaryRoleCode() == RoleName.SUPER_ADMIN) {
            throw new BadRequestException("Super admin accounts cannot be moderated from this endpoint.");
        }
        
        target.setAccountStatus(status);
        
        // Handle blocking
        if (status == AccountStatus.BLOCKED) {
            target.setBlockReason(message == null || message.isBlank() ? "Your account has been blocked by the admin team." : message.trim());
            target.setBlockedAt(OffsetDateTime.now());
            
            // Delete all listings if owner is blocked
            if (target.getPrimaryRoleCode() == RoleName.OWNER) {
                listingRepository.deleteByOwnerUser(target);
            }
            
            notificationService.notifyUser(
                    target,
                    NotificationType.ACTION_REQUIRED,
                    "Account blocked",
                    target.getBlockReason(),
                    "/dashboard",
                    null
            );
        } else {
            // Clear block reason if status is not BLOCKED
            target.setBlockReason(null);
            target.setBlockedAt(null);
        }
        
        // Handle deletion
        if (status == AccountStatus.DELETED) {
            target.setDeletedAt(OffsetDateTime.now());
        } else {
            target.setDeletedAt(null);
        }
        
        // Handle warning
        if (status == AccountStatus.WARNING) {
            notificationService.notifyUser(
                    target,
                    NotificationType.WARNING,
                    "Admin warning",
                    message == null || message.isBlank() ? "An admin has issued a warning on your account." : message.trim(),
                    "/dashboard",
                    null
            );
        }
        
        ModeratedUserResponse response = mapModeratedUser(userRepository.save(target));
        realtimeEventPublisher.publishRoleEvent("ADMIN", "user_status_updated", response);
        realtimeEventPublisher.publishRoleEvent("SUPER_ADMIN", "user_status_updated", response);
        return response;
    }

    @Transactional
    public void deleteUser(UUID reviewerId, UUID userId) {
        requireSuperAdmin(reviewerId);
        updateUserStatus(reviewerId, userId, AccountStatus.DELETED, "Your account has been deleted by the super admin.");
    }

    @Transactional
    public ConnectedAdminResponse revokeAdminAccess(UUID reviewerId, UUID adminUserId, String reason) {
        requireSuperAdmin(reviewerId);
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found."));
        
        if (adminUser.getPrimaryRoleCode() != RoleName.ADMIN) {
            throw new BadRequestException("This user is not an admin.");
        }
        
        if (adminUser.getPrimaryRoleCode() == RoleName.SUPER_ADMIN) {
            throw new BadRequestException("Super admin accounts cannot be revoked.");
        }
        
        AdminProfile adminProfile = adminProfileRepository.findByUser(adminUser)
                .orElseThrow(() -> new NotFoundException("Admin profile not found."));
        
        adminProfile.setEmployeeStatus(EmployeeStatus.REVOKED);
        adminProfile.setRevokedAt(OffsetDateTime.now());
        adminProfile.setRevokeReason(reason == null || reason.isBlank() 
                ? "Your access has been revoked by Super Admin" 
                : reason.trim());
        
        adminProfileRepository.save(adminProfile);
        
        notificationService.notifyUser(
                adminUser,
                NotificationType.ACTION_REQUIRED,
                "Access Revoked",
                adminProfile.getRevokeReason(),
                "/dashboard",
                null
        );
        
        ConnectedAdminResponse response = new ConnectedAdminResponse(
                adminUser.getId(),
                adminUser.getUserCode(),
                adminUser.getDisplayName(),
                adminUser.getEmail(),
                adminProfile.getCity() == null ? "All Cities" : adminProfile.getCity().getName(),
                adminUser.getAccountStatus(),
                adminProfile.getEmployeeStatus(),
                adminProfile.isCanManageAllCities()
        );
        
        realtimeEventPublisher.publishRoleEvent("SUPER_ADMIN", "admin_revoked", response);
        realtimeEventPublisher.publishUserEvent(adminUser.getId().toString(), "access_revoked", 
                java.util.Map.of("reason", adminProfile.getRevokeReason()));
        
        return response;
    }

    @Transactional
    public ConnectedAdminResponse activateAdminAccess(UUID reviewerId, UUID adminUserId) {
        requireSuperAdmin(reviewerId);
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found."));
        
        if (adminUser.getPrimaryRoleCode() != RoleName.ADMIN) {
            throw new BadRequestException("This user is not an admin.");
        }
        
        AdminProfile adminProfile = adminProfileRepository.findByUser(adminUser)
                .orElseThrow(() -> new NotFoundException("Admin profile not found."));
        
        adminProfile.setEmployeeStatus(EmployeeStatus.ACTIVE);
        adminProfile.setRevokedAt(null);
        adminProfile.setRevokeReason(null);
        
        adminProfileRepository.save(adminProfile);
        
        notificationService.notifyUser(
                adminUser,
                NotificationType.INFO,
                "Access Restored",
                "Your admin access has been restored by Super Admin. You can now login and access your dashboard.",
                "/dashboard",
                null
        );
        
        ConnectedAdminResponse response = new ConnectedAdminResponse(
                adminUser.getId(),
                adminUser.getUserCode(),
                adminUser.getDisplayName(),
                adminUser.getEmail(),
                adminProfile.getCity() == null ? "All Cities" : adminProfile.getCity().getName(),
                adminUser.getAccountStatus(),
                adminProfile.getEmployeeStatus(),
                adminProfile.isCanManageAllCities()
        );
        
        realtimeEventPublisher.publishRoleEvent("SUPER_ADMIN", "admin_activated", response);
        realtimeEventPublisher.publishUserEvent(adminUser.getId().toString(), "access_restored", 
                java.util.Map.of("message", "Your access has been restored"));
        
        return response;
    }

    @Transactional
    public void deleteAdmin(UUID reviewerId, UUID adminUserId) {
        requireSuperAdmin(reviewerId);
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found."));
        
        if (adminUser.getPrimaryRoleCode() != RoleName.ADMIN) {
            throw new BadRequestException("This user is not an admin.");
        }
        
        if (adminUser.getPrimaryRoleCode() == RoleName.SUPER_ADMIN) {
            throw new BadRequestException("Super admin accounts cannot be deleted.");
        }
        
        // Soft delete the admin
        adminUser.setAccountStatus(AccountStatus.DELETED);
        adminUser.setDeletedAt(OffsetDateTime.now());
        userRepository.save(adminUser);
        
        realtimeEventPublisher.publishRoleEvent("SUPER_ADMIN", "admin_deleted", 
                java.util.Map.of("userId", adminUserId, "userCode", adminUser.getUserCode()));
    }

    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats(UUID reviewerId) {
        requireSuperAdmin(reviewerId);
        long students = userRepository.findByPrimaryRoleCodeOrderByCreatedAtDesc(RoleName.STUDENT).stream().filter(this::isVisible).count();
        long owners = userRepository.findByPrimaryRoleCodeOrderByCreatedAtDesc(RoleName.OWNER).stream().filter(this::isVisible).count();
        long admins = userRepository.findByPrimaryRoleCodeOrderByCreatedAtDesc(RoleName.ADMIN).stream().filter(this::isVisible).count();
        return new PlatformStatsResponse(students, owners, admins);
    }

    @Transactional(readOnly = true)
    public List<ConnectedAdminResponse> getConnectedAdmins(UUID reviewerId) {
        requireSuperAdmin(reviewerId);
        return adminProfileRepository.findAll().stream()
                .filter(profile -> isVisible(profile.getUser()))
                .map(profile -> new ConnectedAdminResponse(
                        profile.getUser().getId(),
                        profile.getUser().getUserCode(),
                        profile.getUser().getDisplayName(),
                        profile.getUser().getEmail(),
                        profile.getCity() == null ? "All Cities" : profile.getCity().getName(),
                        profile.getUser().getAccountStatus(),
                        profile.getEmployeeStatus(),
                        profile.isCanManageAllCities()
                ))
                .sorted(Comparator.comparing(ConnectedAdminResponse::userCode))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagedStudentResponse> getAllStudents(UUID reviewerId) {
        requireSuperAdmin(reviewerId);
        return studentProfileRepository.findAll().stream()
                .filter(profile -> isVisible(profile.getUser()))
                .map(this::mapStudent)
                .sorted(Comparator.comparing(ManagedStudentResponse::displayName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagedOwnerResponse> getAllOwners(UUID reviewerId) {
        requireSuperAdmin(reviewerId);
        return ownerProfileRepository.findAll().stream()
                .filter(profile -> isVisible(profile.getUser()))
                .map(this::mapOwner)
                .sorted(Comparator.comparing(ManagedOwnerResponse::displayName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CityAnalyticsResponse> getCityAnalytics(UUID reviewerId) {
        requireSuperAdmin(reviewerId);
        List<StudentProfile> students = studentProfileRepository.findAll().stream().filter(profile -> isVisible(profile.getUser())).toList();
        List<OwnerProfile> owners = ownerProfileRepository.findAll().stream().filter(profile -> isVisible(profile.getUser())).toList();
        List<Listing> listings = listingRepository.findAll().stream().filter(listing -> isVisible(listing.getOwnerUser())).toList();
        return cityRepository.findAll().stream()
                .map(city -> new CityAnalyticsResponse(
                        city.getId(),
                        city.getName(),
                        listings.stream().filter(listing -> matchesCity(city.getName(), listing.getCity(), listing.getLocality())).count(),
                        owners.stream().filter(profile -> matchesCity(city.getName(), profile.getCity(), profile.getLocality())).count(),
                        students.stream().filter(profile -> matchesCity(city.getName(), profile.getCity(), profile.getCurrentLocation())).count()
                ))
                .sorted(Comparator.comparing(CityAnalyticsResponse::cityName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectedAdminResponse> getActiveAdminsForOwner(UUID ownerId) {
        User owner = currentUserService.requireUser(ownerId);
        
        // Get all active admins (not revoked, not deleted, and employee status is ACTIVE)
        // Exclude SUPER_ADMIN - they only manage cities and admins, not verify listings
        return adminProfileRepository.findAll().stream()
                .filter(profile -> isVisible(profile.getUser()))
                .filter(profile -> profile.getUser().getAccountStatus() == AccountStatus.ACTIVE)
                .filter(profile -> profile.getEmployeeStatus() == EmployeeStatus.ACTIVE)
                .filter(profile -> profile.getUser().getPrimaryRoleCode() != RoleName.SUPER_ADMIN)
                .map(profile -> new ConnectedAdminResponse(
                        profile.getUser().getId(),
                        profile.getUser().getUserCode(),
                        profile.getUser().getDisplayName(),
                        profile.getUser().getEmail(),
                        profile.getCity() == null ? "All Cities" : profile.getCity().getName(),
                        profile.getUser().getAccountStatus(),
                        profile.getEmployeeStatus(),
                        profile.isCanManageAllCities()
                ))
                .sorted(Comparator.comparing(ConnectedAdminResponse::userCode))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConnectedAdminResponse findMatchingAdminForOwner(UUID ownerId) {
        User owner = currentUserService.requireUser(ownerId);
        OwnerProfile ownerProfile = ownerProfileRepository.findByUser(owner).orElse(null);
        
        if (ownerProfile == null || ownerProfile.getCity() == null) {
            return null;
        }
        
        String ownerCityName = ownerProfile.getCity().getName();
        
        // Find active admin matching owner's city
        // Exclude SUPER_ADMIN - they only manage cities and admins, not verify listings
        return adminProfileRepository.findAll().stream()
                .filter(profile -> isVisible(profile.getUser()))
                .filter(profile -> profile.getUser().getAccountStatus() == AccountStatus.ACTIVE)
                .filter(profile -> profile.getEmployeeStatus() == EmployeeStatus.ACTIVE)
                .filter(profile -> profile.getUser().getPrimaryRoleCode() != RoleName.SUPER_ADMIN)
                .filter(profile -> {
                    // Match if admin manages all cities OR admin's city matches owner's city
                    if (profile.isCanManageAllCities()) {
                        return true;
                    }
                    if (profile.getCity() != null && profile.getCity().getName().equalsIgnoreCase(ownerCityName)) {
                        return true;
                    }
                    return false;
                })
                .map(profile -> new ConnectedAdminResponse(
                        profile.getUser().getId(),
                        profile.getUser().getUserCode(),
                        profile.getUser().getDisplayName(),
                        profile.getUser().getEmail(),
                        profile.getCity() == null ? "All Cities" : profile.getCity().getName(),
                        profile.getUser().getAccountStatus(),
                        profile.getEmployeeStatus(),
                        profile.isCanManageAllCities()
                ))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public CityAnalyticsResponse createCity(UUID reviewerId, CityCreateRequest request) {
        requireSuperAdmin(reviewerId);
        String cityName = request.cityName().trim();
        String state = request.state().trim();
        String country = request.country().trim();
        cityRepository.findByNameIgnoreCaseAndStateIgnoreCaseAndCountryIgnoreCase(cityName, state, country)
                .ifPresent(existing -> {
                    throw new BadRequestException("This city is already available.");
                });

        City city = new City();
        city.setName(cityName);
        city.setState(state);
        city.setCountry(country);

        City savedCity = cityRepository.save(city);
        CityAnalyticsResponse response = new CityAnalyticsResponse(savedCity.getId(), savedCity.getName(), 0, 0, 0);
        realtimeEventPublisher.publishRoleEvent("SUPER_ADMIN", "city_created", response);
        return response;
    }

    private void publishListingModerationEvent(String eventType, Listing listing, PendingListingResponse adminPayload) {
        realtimeEventPublisher.publishRoleEvent("ADMIN", eventType, adminPayload);
        realtimeEventPublisher.publishRoleEvent("SUPER_ADMIN", eventType, adminPayload);
        java.util.Map<String, Object> ownerPayload = new java.util.LinkedHashMap<>();
        ownerPayload.put("id", listing.getId());
        ownerPayload.put("title", listing.getTitle());
        ownerPayload.put("location", listing.getLocality() != null ? listing.getLocality() : listing.getAddressLineOne());
        ownerPayload.put("rentAmount", listing.getRentAmount());
        ownerPayload.put("totalCapacity", listing.getTotalCapacity());
        ownerPayload.put("availableCapacity", listing.getAvailableCapacity());
        ownerPayload.put("status", listing.getStatus());
        ownerPayload.put("rejectionReason", listing.getRejectionReason());
        ownerPayload.put("latestFakeDetectionStatus", listing.getLatestFakeDetectionStatus());
        ownerPayload.put("media", listingMediaRepository.findByListingOrderBySortOrderAsc(listing).stream()
                .map(media -> java.util.Map.of(
                        "url", media.getUrl(),
                        "mediaType", media.getMediaType(),
                        "primary", media.isPrimary()
                ))
                .toList());
        realtimeEventPublisher.publishUserEvent(listing.getOwnerUser().getId().toString(), eventType, ownerPayload);
    }

    private ManagedStudentResponse mapStudent(StudentProfile profile) {
        User user = profile.getUser();
        return new ManagedStudentResponse(
                user.getId(),
                user.getUserCode(),
                user.getDisplayName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.isIdentityVerified(),
                user.getAccountStatus(),
                bestCityLabel(profile.getCity(), profile.getCurrentLocation()),
                complaintRepository.countByAgainstUserAndStatusIn(user, ACTIVE_COMPLAINT_STATUSES),
                complaintRepository.countByAgainstUserAndStatusIn(user, RESOLVED_COMPLAINT_STATUSES),
                profile.getCollegeName(),
                profile.getPrn(),
                user.getCompletionPercentage()
        );
    }

    private ManagedOwnerResponse mapOwner(OwnerProfile profile) {
        User user = profile.getUser();
        Listing latestListing = listingRepository.findByOwnerUserOrderByCreatedAtDesc(user).stream().findFirst().orElse(null);
        return new ManagedOwnerResponse(
                user.getId(),
                user.getUserCode(),
                user.getDisplayName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.isIdentityVerified(),
                user.getAccountStatus(),
                latestListing == null ? null : latestListing.getTitle(),
                latestListing == null ? null : latestListing.getStatus(),
                latestListing != null && latestListing.getLocality() != null ? latestListing.getLocality() : bestCityLabel(profile.getCity(), profile.getLocality()),
                complaintRepository.countByAgainstUserAndStatusIn(user, ACTIVE_COMPLAINT_STATUSES),
                complaintRepository.countByAgainstUserAndStatusIn(user, RESOLVED_COMPLAINT_STATUSES),
                profile.getPanNumber(),
                user.getCompletionPercentage()
        );
    }

    private PendingListingResponse mapPendingListing(Listing listing) {
        List<ListingMedia> media = listingMediaRepository.findByListingOrderBySortOrderAsc(listing);
        
        // Get the owner photo from listing media (uploaded during listing creation)
        String ownerPhotoUrl = media.stream()
                .filter(item -> item.getMediaType() == MediaType.OWNER_PHOTO)
                .map(ListingMedia::getUrl)
                .findFirst()
                .orElse(null);
        
        return new PendingListingResponse(
                listing.getId(),
                listing.getOwnerUser().getDisplayName(),
                listing.getOwnerUser().getUserCode(),
                ownerPhotoUrl, // Use uploaded owner photo, NOT profile photo
                listing.getTitle(),
                listing.getStatus(),
                listing.getLatestFakeDetectionStatus(),
                media.stream().filter(item -> item.getMediaType() == MediaType.IMAGE).map(ListingMedia::getUrl).toList(),
                media.stream().filter(item -> item.getMediaType() == MediaType.VIDEO).map(ListingMedia::getUrl).findFirst().orElse(null)
        );
    }

    private AdminScope resolveAdminScope(UUID reviewerId) {
        User reviewer = requireAdminOrSuperAdmin(reviewerId);
        if (reviewer.getPrimaryRoleCode() == RoleName.SUPER_ADMIN) {
            return new AdminScope(false, "All Cities", null, null);
        }
        AdminProfile adminProfile = adminProfileRepository.findByUser(reviewer)
                .orElseThrow(() -> new NotFoundException("Admin profile not found."));
        if (adminProfile.isCanManageAllCities() || adminProfile.getCity() == null) {
            return new AdminScope(false, adminProfile.getCity() == null ? "All Cities" : adminProfile.getCity().getName(), adminProfile.getCity(), reviewerId);
        }
        return new AdminScope(true, adminProfile.getCity().getName(), adminProfile.getCity(), reviewerId);
    }

    private Listing requireListingForAdminScope(UUID reviewerId, UUID listingId) {
        AdminScope scope = resolveAdminScope(reviewerId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        
        // Priority 1: If listing has an assigned admin, check if it matches the reviewer
        if (listing.getAssignedAdmin() != null) {
            if (scope.adminUserId() == null || !listing.getAssignedAdmin().getId().equals(scope.adminUserId())) {
                throw new BadRequestException("This listing is not assigned to you.");
            }
            return listing; // Admin is authorized via assignment
        }
        
        // Priority 2: For legacy listings without assigned admin, check city scope
        if (scope.restricted() && !matchesCity(scope.city().getName(), listing.getCity(), listing.getLocality())) {
            throw new BadRequestException("This listing is outside your allotted city.");
        }
        
        return listing;
    }

    private User requireAdminOrSuperAdmin(UUID reviewerId) {
        User reviewer = currentUserService.requireUser(reviewerId);
        if (reviewer.getPrimaryRoleCode() != RoleName.ADMIN && reviewer.getPrimaryRoleCode() != RoleName.SUPER_ADMIN) {
            throw new BadRequestException("Only admins can perform this action.");
        }
        return reviewer;
    }

    private void requireSuperAdmin(UUID reviewerId) {
        User reviewer = currentUserService.requireUser(reviewerId);
        if (reviewer.getPrimaryRoleCode() != RoleName.SUPER_ADMIN) {
            throw new BadRequestException("Only super admins can perform this action.");
        }
    }

    private List<StudentProfile> loadScopedStudents(AdminScope scope) {
        return studentProfileRepository.findAll().stream()
                .filter(profile -> isVisible(profile.getUser()))
                .filter(profile -> !scope.restricted() || matchesCity(scope.city().getName(), profile.getCity(), profile.getCurrentLocation()))
                .toList();
    }

    private List<OwnerProfile> loadScopedOwners(AdminScope scope) {
        return ownerProfileRepository.findAll().stream()
                .filter(profile -> isVisible(profile.getUser()))
                .filter(profile -> !scope.restricted() || matchesCity(scope.city().getName(), profile.getCity(), profile.getLocality()))
                .toList();
    }

    private List<Listing> loadScopedListings(AdminScope scope) {
        return listingRepository.findAll().stream()
                .filter(listing -> isVisible(listing.getOwnerUser()))
                .filter(listing -> {
                    // Priority 1: If listing has an assigned admin, show ONLY to that admin
                    if (listing.getAssignedAdmin() != null) {
                        return scope.adminUserId() != null && 
                               listing.getAssignedAdmin().getId().equals(scope.adminUserId());
                    }
                    
                    // Priority 2: For listings without assigned admin (legacy), use city-based filtering
                    return !scope.restricted() || matchesCity(scope.city().getName(), listing.getCity(), listing.getLocality());
                })
                .toList();
    }

    private boolean isVisible(User user) {
        return user.getAccountStatus() != AccountStatus.DELETED;
    }

    private boolean matchesCity(String cityName, City city, String freeText) {
        if (city != null && city.getName() != null && city.getName().equalsIgnoreCase(cityName)) {
            return true;
        }
        return freeText != null && freeText.toLowerCase().contains(cityName.toLowerCase());
    }

    private String bestCityLabel(City city, String fallback) {
        if (city != null && city.getName() != null) {
            return city.getName();
        }
        return fallback == null || fallback.isBlank() ? "N/A" : fallback;
    }

    private ModeratedUserResponse mapModeratedUser(User user) {
        return new ModeratedUserResponse(
                user.getId(),
                user.getUserCode(),
                user.getDisplayName(),
                user.getPrimaryRoleCode(),
                user.getAccountStatus(),
                user.getDeletedAt()
        );
    }

    private record AdminScope(boolean restricted, String cityName, City city, UUID adminUserId) {
    }
}
