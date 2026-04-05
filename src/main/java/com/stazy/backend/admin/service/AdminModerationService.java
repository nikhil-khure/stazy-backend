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
import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.enums.ComplaintStatus;
import com.stazy.backend.common.enums.ListingStatus;
import com.stazy.backend.common.enums.MediaType;
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
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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

    public AdminModerationService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            OwnerProfileRepository ownerProfileRepository,
            AdminProfileRepository adminProfileRepository,
            ListingRepository listingRepository,
            ListingMediaRepository listingMediaRepository,
            ComplaintRepository complaintRepository,
            CityRepository cityRepository
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
    public PendingListingResponse goLive(UUID reviewerId, UUID listingId) {
        Listing listing = requireListingForAdminScope(reviewerId, listingId);
        listing.setStatus(ListingStatus.LIVE);
        listing.setPublishedAt(OffsetDateTime.now());
        listing.setRejectionReason(null);
        return mapPendingListing(listingRepository.save(listing));
    }

    @Transactional
    public PendingListingResponse rejectListing(UUID reviewerId, UUID listingId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Rejection reason is required.");
        }
        Listing listing = requireListingForAdminScope(reviewerId, listingId);
        listing.setStatus(ListingStatus.REJECTED);
        listing.setRejectionReason(reason.trim());
        listing.setPublishedAt(null);
        return mapPendingListing(listingRepository.save(listing));
    }

    @Transactional
    public ModeratedUserResponse updateUserStatus(UUID reviewerId, UUID userId, AccountStatus status) {
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
        target.setDeletedAt(status == AccountStatus.DELETED ? OffsetDateTime.now() : null);
        return mapModeratedUser(userRepository.save(target));
    }

    @Transactional
    public void deleteUser(UUID reviewerId, UUID userId) {
        requireSuperAdmin(reviewerId);
        updateUserStatus(reviewerId, userId, AccountStatus.DELETED);
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
        return new CityAnalyticsResponse(savedCity.getId(), savedCity.getName(), 0, 0, 0);
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
        return new PendingListingResponse(
                listing.getId(),
                listing.getOwnerUser().getDisplayName(),
                listing.getOwnerUser().getUserCode(),
                listing.getOwnerUser().getProfilePhotoUrl(),
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
            return new AdminScope(false, "All Cities", null);
        }
        AdminProfile adminProfile = adminProfileRepository.findByUser(reviewer)
                .orElseThrow(() -> new NotFoundException("Admin profile not found."));
        if (adminProfile.isCanManageAllCities() || adminProfile.getCity() == null) {
            return new AdminScope(false, adminProfile.getCity() == null ? "All Cities" : adminProfile.getCity().getName(), adminProfile.getCity());
        }
        return new AdminScope(true, adminProfile.getCity().getName(), adminProfile.getCity());
    }

    private Listing requireListingForAdminScope(UUID reviewerId, UUID listingId) {
        AdminScope scope = resolveAdminScope(reviewerId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
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
                .filter(listing -> !scope.restricted() || matchesCity(scope.city().getName(), listing.getCity(), listing.getLocality()))
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

    private record AdminScope(boolean restricted, String cityName, City city) {
    }
}
