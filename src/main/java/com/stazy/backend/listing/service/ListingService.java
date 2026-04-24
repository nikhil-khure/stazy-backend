package com.stazy.backend.listing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stazy.backend.admin.dto.ConnectedAdminResponse;
import com.stazy.backend.admin.service.AdminModerationService;
import com.stazy.backend.booking.entity.ActiveStay;
import com.stazy.backend.booking.entity.RentPayment;
import com.stazy.backend.booking.repository.ActiveStayRepository;
import com.stazy.backend.booking.repository.RentPaymentRepository;
import com.stazy.backend.common.api.PageResponse;
import com.stazy.backend.common.events.RealtimeEventPublisher;
import com.stazy.backend.common.enums.ListingStatus;
import com.stazy.backend.common.enums.MediaType;
import com.stazy.backend.common.enums.VerificationStatus;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.complaint.repository.ComplaintRepository;
import com.stazy.backend.integration.cloudinary.CloudinaryService;
import com.stazy.backend.integration.cloudinary.UploadedAsset;
import com.stazy.backend.listing.dto.ListingMediaResponse;
import com.stazy.backend.listing.dto.ListingResponse;
import com.stazy.backend.listing.dto.ListingUpsertRequest;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.listing.entity.ListingMedia;
import com.stazy.backend.listing.repository.ListingMediaRepository;
import com.stazy.backend.listing.repository.ListingRepository;
import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.profile.repository.OwnerProfileRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.CurrentUserService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingMediaRepository listingMediaRepository;
    private final CurrentUserService currentUserService;
    private final CloudinaryService cloudinaryService;
    private final OwnerProfileRepository ownerProfileRepository;
    private final ObjectMapper objectMapper;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ActiveStayRepository activeStayRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final ComplaintRepository complaintRepository;
    private final AdminModerationService adminModerationService;
    private final UserRepository userRepository;

    public ListingService(
            ListingRepository listingRepository,
            ListingMediaRepository listingMediaRepository,
            CurrentUserService currentUserService,
            CloudinaryService cloudinaryService,
            OwnerProfileRepository ownerProfileRepository,
            ObjectMapper objectMapper,
            RealtimeEventPublisher realtimeEventPublisher,
            ActiveStayRepository activeStayRepository,
            RentPaymentRepository rentPaymentRepository,
            ComplaintRepository complaintRepository,
            AdminModerationService adminModerationService,
            UserRepository userRepository
    ) {
        this.listingRepository = listingRepository;
        this.listingMediaRepository = listingMediaRepository;
        this.currentUserService = currentUserService;
        this.cloudinaryService = cloudinaryService;
        this.ownerProfileRepository = ownerProfileRepository;
        this.objectMapper = objectMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.activeStayRepository = activeStayRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.complaintRepository = complaintRepository;
        this.adminModerationService = adminModerationService;
        this.userRepository = userRepository;
    }

    @Cacheable("listing-search")
    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> search(
            String search,
            String location,
            String priceBucket,
            String roomKind,
            String genderCategory,
            BigDecimal minimumRating,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Specification<Listing> specification = Specification.where(hasLiveStatus())
                .and(matchesSearch(search))
                .and(matchesLocation(location))
                .and(matchesPriceBucket(priceBucket))
                .and(matchesRoomKind(roomKind))
                .and(matchesGenderCategory(genderCategory))
                .and(matchesMinimumRating(minimumRating));

        Page<Listing> result = listingRepository.findAll(specification, pageable);
        List<ListingResponse> items = result.getContent().stream().map(this::mapListing).toList();
        return new PageResponse<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Cacheable("listing-detail")
    @Transactional(readOnly = true)
    public ListingResponse getById(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        return mapListing(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getOwnerListings(UUID ownerId) {
        User owner = currentUserService.requireUser(ownerId);
        return listingRepository.findByOwnerUserOrderByCreatedAtDesc(owner).stream()
                .map(this::mapListing)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"listing-search", "listing-detail"}, allEntries = true)
    public ListingResponse create(UUID ownerId, ListingUpsertRequest request) {
        validateListingRequest(request);
        User owner = currentUserService.requireUser(ownerId);
        ensureVerifiedAccess(owner);
        
        // Owner photo must be uploaded with the listing request
        if (request.getOwnerPhoto() == null || request.getOwnerPhoto().isEmpty()) {
            throw new BadRequestException("Owner photo is required for listing verification.");
        }
        
        OwnerProfile ownerProfile = ownerProfileRepository.findByUser(owner)
                .orElseThrow(() -> new BadRequestException("Owner profile is required before creating a listing."));

        // Requirement 32: Enforce location restriction - owner can only create listings in their registered city
        if (ownerProfile.getCity() != null) {
            String ownerCityName = ownerProfile.getCity().getName();
            String listingLocation = request.getLocation() != null ? request.getLocation().toLowerCase() : "";
            if (!listingLocation.contains(ownerCityName.toLowerCase())) {
                throw new BadRequestException("You can only create listings in your registered city: " + ownerCityName);
            }
        }

        // Assign admin to the listing
        User assignedAdmin = assignAdminToListing(ownerId, request.getSelectedAdminId());

        Listing listing = new Listing();
        listing.setOwnerUser(owner);
        listing.setAssignedAdmin(assignedAdmin);
        applyRequestToListing(listing, request, ownerProfile);
        listing.setAvailableCapacity(request.getTotalCapacity());
        listing.setStatus(ListingStatus.UNDER_REVIEW);
        listing.setLatestFakeDetectionStatus(VerificationStatus.PENDING);
        Listing saved = listingRepository.save(listing);
        replaceMedia(saved, List.of(), null, request.getRoomImages(), request.getOwnerPhoto(), request.getOwnerLiveVideo(), ownerId);
        ListingResponse response = mapListing(saved);
        publishListingEvents("listing_created", owner, saved, response);
        return response;
    }
    
    private User assignAdminToListing(UUID ownerId, String selectedAdminId) {
        // If admin was manually selected by owner, use that admin
        if (selectedAdminId != null && !selectedAdminId.isBlank()) {
            UUID adminId = UUID.fromString(selectedAdminId);
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new BadRequestException("Selected admin not found."));
            return admin;
        }
        
        // Otherwise, try to find a matching admin automatically
        ConnectedAdminResponse matchingAdmin = adminModerationService.findMatchingAdminForOwner(ownerId);
        if (matchingAdmin != null) {
            return userRepository.findById(matchingAdmin.userId())
                    .orElseThrow(() -> new BadRequestException("Matching admin not found."));
        }
        
        // If no matching admin, throw error - frontend should have shown selection popup
        throw new BadRequestException("No admin assigned. Please select an admin for your listing.");
    }

    @Transactional
    @CacheEvict(cacheNames = {"listing-search", "listing-detail"}, allEntries = true)
    public ListingResponse update(UUID ownerId, UUID listingId, ListingUpsertRequest request) {
        validateListingRequest(request);
        User owner = currentUserService.requireUser(ownerId);
        ensureVerifiedAccess(owner);
        OwnerProfile ownerProfile = ownerProfileRepository.findByUser(owner)
                .orElseThrow(() -> new BadRequestException("Owner profile is required before updating a listing."));
        Listing listing = listingRepository.findByIdAndOwnerUser(listingId, owner)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        
        // Check if media is being changed (for conditional review flow)
        boolean mediaChanged = false;
        if (request.getRoomImages() != null
                || request.getOwnerPhoto() != null
                || request.getOwnerLiveVideo() != null
                || request.getExistingRoomImageUrlsJson() != null
                || request.getExistingOwnerVideoUrl() != null) {
            mediaChanged = hasMediaChanged(
                    listing,
                    parseExistingRoomImageUrls(request.getExistingRoomImageUrlsJson()),
                    blankToNull(request.getExistingOwnerVideoUrl()),
                    request.getRoomImages(),
                    request.getOwnerPhoto(),
                    request.getOwnerLiveVideo()
            );
        }
        
        applyRequestToListing(listing, request, ownerProfile);
        
        // If media changed and listing was LIVE, move to UNDER_REVIEW for re-verification
        if (mediaChanged && listing.getStatus() == ListingStatus.LIVE) {
            listing.setStatus(ListingStatus.UNDER_REVIEW);
            listing.setLatestFakeDetectionStatus(VerificationStatus.PENDING);
        }
        
        if (request.getRoomImages() != null
                || request.getOwnerPhoto() != null
                || request.getOwnerLiveVideo() != null
                || request.getExistingRoomImageUrlsJson() != null
                || request.getExistingOwnerVideoUrl() != null) {
            replaceMedia(
                    listing,
                    parseExistingRoomImageUrls(request.getExistingRoomImageUrlsJson()),
                    blankToNull(request.getExistingOwnerVideoUrl()),
                    request.getRoomImages(),
                    request.getOwnerPhoto(),
                    request.getOwnerLiveVideo(),
                    ownerId
            );
        }
        Listing savedListing = listingRepository.save(listing);
        ListingResponse response = mapListing(savedListing);
        publishListingEvents("listing_updated", owner, savedListing, response);
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = {"listing-search", "listing-detail"}, allEntries = true)
    public void delete(UUID ownerId, UUID listingId) {
        User owner = currentUserService.requireUser(ownerId);
        Listing listing = listingRepository.findByIdAndOwnerUser(listingId, owner)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        
        // Delete all active stays related to this listing
        List<ActiveStay> activeStays = activeStayRepository.findByOwnerUserOrderByCreatedAtDesc(owner)
                .stream()
                .filter(stay -> stay.getListing().getId().equals(listingId))
                .toList();
        
        for (ActiveStay stay : activeStays) {
            User student = stay.getStudentUser();
            
            // Delete all complaints between owner and student
            complaintRepository.deleteByComplainantUserAndAgainstUser(owner, student);
            complaintRepository.deleteByAgainstUserAndComplainantUser(owner, student);
            
            // Delete all payments for this stay
            rentPaymentRepository.findByActiveStayOrderByPeriodStartDesc(stay)
                    .forEach(rentPaymentRepository::delete);
            
            // Delete the active stay
            activeStayRepository.delete(stay);
        }
        
        ListingResponse response = mapListing(listing);
        listingMediaRepository.deleteByListing(listing);
        listingRepository.delete(listing);
        publishListingEvents("listing_deleted", owner, listing, response);
    }

    private void publishListingEvents(String eventType, User owner, Listing listing, ListingResponse ownerPayload) {
        List<ListingMedia> media = listingMediaRepository.findByListingOrderBySortOrderAsc(listing);
        realtimeEventPublisher.publishUserEvent(owner.getId().toString(), eventType, ownerPayload);
        Map<String, Object> adminPayload = new java.util.LinkedHashMap<>();
        adminPayload.put("listingId", listing.getId());
        adminPayload.put("ownerName", listing.getOwnerUser().getDisplayName());
        adminPayload.put("ownerUserCode", listing.getOwnerUser().getUserCode());
        adminPayload.put("listingTitle", listing.getTitle());
        adminPayload.put("status", listing.getStatus());
        adminPayload.put("fakeDetectionStatus", listing.getLatestFakeDetectionStatus());
        adminPayload.put("imageUrls", media.stream()
                .filter(item -> item.getMediaType() == MediaType.IMAGE)
                .map(ListingMedia::getUrl)
                .toList());
        adminPayload.put("videoUrl", media.stream()
                .filter(item -> item.getMediaType() == MediaType.VIDEO)
                .map(ListingMedia::getUrl)
                .findFirst()
                .orElse(null));
        realtimeEventPublisher.publishRoleEvent("ADMIN", eventType, adminPayload);
        realtimeEventPublisher.publishRoleEvent("SUPER_ADMIN", eventType, adminPayload);
    }

    private void applyRequestToListing(Listing listing, ListingUpsertRequest request, OwnerProfile ownerProfile) {
        listing.setTitle(request.getTitle().trim());
        listing.setDescription(blankToNull(request.getDescription()));
        listing.setAddressLineOne(request.getLocation().trim());
        listing.setLocality(ownerProfile.getCity() == null ? request.getLocation().trim() : ownerProfile.getCity().getName());
        listing.setCity(ownerProfile.getCity());
        listing.setPincode(ownerProfile.getPincode());
        listing.setRentAmount(request.getRentAmount());
        listing.setRoomKind(request.getRoomKind());
        listing.setGenderCategory(request.getGenderCategory());
        listing.setTotalCapacity(request.getTotalCapacity());
        listing.setAvailableCapacity(Math.min(listing.getAvailableCapacity() == null ? request.getTotalCapacity() : listing.getAvailableCapacity(), request.getTotalCapacity()));
        listing.setAmenities(parseAmenities(request.getAmenitiesCsv()));
        if (listing.getStatus() == ListingStatus.REJECTED) {
            listing.setStatus(ListingStatus.UNDER_REVIEW);
            listing.setRejectionReason(null);
        }
    }

    private void replaceMedia(
            Listing listing,
            List<String> existingRoomImageUrls,
            String existingOwnerVideoUrl,
            MultipartFile[] roomImages,
            MultipartFile ownerPhoto,
            MultipartFile ownerLiveVideo,
            UUID ownerId
    ) {
        List<ListingMedia> currentMedia = listingMediaRepository.findByListingOrderBySortOrderAsc(listing);
        Set<String> keptRoomImageUrls = new LinkedHashSet<>(existingRoomImageUrls == null ? List.of() : existingRoomImageUrls);
        ListingMedia keptVideo = currentMedia.stream()
                .filter(media -> media.getMediaType() == MediaType.VIDEO)
                .filter(media -> existingOwnerVideoUrl != null && existingOwnerVideoUrl.equals(media.getUrl()))
                .findFirst()
                .orElse(null);

        currentMedia.stream()
                .filter(media -> media.getMediaType() == MediaType.IMAGE && !keptRoomImageUrls.contains(media.getUrl()))
                .forEach(listingMediaRepository::delete);
        currentMedia.stream()
                .filter(media -> media.getMediaType() == MediaType.VIDEO)
                .filter(media -> keptVideo == null || !media.getId().equals(keptVideo.getId()))
                .forEach(listingMediaRepository::delete);

        int sortOrder = 0;
        boolean primaryAssigned = false;

        for (ListingMedia existingImage : currentMedia.stream()
                .filter(media -> media.getMediaType() == MediaType.IMAGE)
                .filter(media -> keptRoomImageUrls.contains(media.getUrl()))
                .toList()) {
            existingImage.setSortOrder(sortOrder++);
            existingImage.setPrimary(!primaryAssigned);
            primaryAssigned = true;
            listingMediaRepository.save(existingImage);
        }

        if (roomImages != null) {
            for (MultipartFile image : roomImages) {
                if (image == null || image.isEmpty()) {
                    continue;
                }
                UploadedAsset uploadedAsset = cloudinaryService.uploadImage(image, "stazy/listings/" + listing.getId());
                ListingMedia media = new ListingMedia();
                media.setListing(listing);
                media.setMediaType(MediaType.IMAGE);
                media.setUrl(uploadedAsset.url());
                media.setPublicId(uploadedAsset.publicId());
                media.setMimeType(image.getContentType());
                media.setFileSizeBytes(image.getSize());
                media.setSortOrder(sortOrder++);
                media.setPrimary(!primaryAssigned);
                primaryAssigned = true;
                media.setUploadedBy(currentUserService.requireUser(ownerId));
                listingMediaRepository.save(media);
            }
        }
        
        // Upload owner photo
        if (ownerPhoto != null && !ownerPhoto.isEmpty()) {
            UploadedAsset uploadedAsset = cloudinaryService.uploadImage(ownerPhoto, "stazy/listings/" + listing.getId() + "/owner");
            ListingMedia media = new ListingMedia();
            media.setListing(listing);
            media.setMediaType(MediaType.OWNER_PHOTO);
            media.setUrl(uploadedAsset.url());
            media.setPublicId(uploadedAsset.publicId());
            media.setMimeType(ownerPhoto.getContentType());
            media.setFileSizeBytes(ownerPhoto.getSize());
            media.setSortOrder(sortOrder++);
            media.setPrimary(false);
            media.setUploadedBy(currentUserService.requireUser(ownerId));
            listingMediaRepository.save(media);
        }
        
        if (ownerLiveVideo != null && !ownerLiveVideo.isEmpty()) {
            UploadedAsset uploadedAsset = cloudinaryService.uploadVideo(ownerLiveVideo, "stazy/listings/" + listing.getId());
            ListingMedia media = new ListingMedia();
            media.setListing(listing);
            media.setMediaType(MediaType.VIDEO);
            media.setUrl(uploadedAsset.url());
            media.setPublicId(uploadedAsset.publicId());
            media.setMimeType(ownerLiveVideo.getContentType());
            media.setFileSizeBytes(ownerLiveVideo.getSize());
            media.setSortOrder(sortOrder);
            media.setPrimary(false);
            media.setUploadedBy(currentUserService.requireUser(ownerId));
            listingMediaRepository.save(media);
        } else if (keptVideo != null) {
            keptVideo.setSortOrder(sortOrder);
            keptVideo.setPrimary(false);
            listingMediaRepository.save(keptVideo);
        }
    }

    private ListingResponse mapListing(Listing listing) {
        List<ListingMediaResponse> mediaResponses = listingMediaRepository.findByListingOrderBySortOrderAsc(listing)
                .stream()
                .map(media -> new ListingMediaResponse(media.getUrl(), media.getMediaType(), media.isPrimary()))
                .toList();
        List<String> amenities = listing.getAmenities() == null || !listing.getAmenities().isArray()
                ? List.of()
                : Arrays.stream(objectMapper.convertValue(listing.getAmenities(), String[].class)).toList();
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                buildLocationLabel(listing.getAddressLineOne(), listing.getLocality()),
                listing.getRentAmount(),
                listing.getRatingAverage(),
                listing.getRatingCount() == null ? 0 : listing.getRatingCount(),
                listing.getRoomKind(),
                listing.getGenderCategory(),
                listing.getTotalCapacity(),
                listing.getAvailableCapacity(),
                listing.getStatus(),
                listing.getRejectionReason(),
                listing.getLatestFakeDetectionStatus(),
                listing.getLatestFakeDetectionStatus() == VerificationStatus.SUCCESS,
                amenities,
                mediaResponses
        );
    }

    private JsonNode parseAmenities(String csv) {
        List<String> items = csv == null || csv.isBlank()
                ? List.of()
                : Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
        return objectMapper.valueToTree(items);
    }

    /**
     * Detects if media (images or videos) has been changed in the update request.
     * Returns true if new media is uploaded or existing media is removed.
     */
    private boolean hasMediaChanged(
            Listing listing,
            List<String> existingRoomImageUrls,
            String existingOwnerVideoUrl,
            MultipartFile[] roomImages,
            MultipartFile ownerPhoto,
            MultipartFile ownerLiveVideo
    ) {
        // Get current media from database
        List<ListingMedia> currentMedia = listingMediaRepository.findByListingOrderBySortOrderAsc(listing);
        
        // Get current room image URLs
        Set<String> currentRoomImageUrls = currentMedia.stream()
                .filter(media -> media.getMediaType() == MediaType.IMAGE)
                .map(ListingMedia::getUrl)
                .collect(Collectors.toSet());
        
        // Get current video URL
        String currentVideoUrl = currentMedia.stream()
                .filter(media -> media.getMediaType() == MediaType.VIDEO)
                .map(ListingMedia::getUrl)
                .findFirst()
                .orElse(null);
        
        // Check if new room images are being uploaded
        if (roomImages != null && roomImages.length > 0) {
            for (MultipartFile image : roomImages) {
                if (image != null && !image.isEmpty()) {
                    return true; // New image uploaded
                }
            }
        }
        
        // Check if new owner photo is being uploaded
        if (ownerPhoto != null && !ownerPhoto.isEmpty()) {
            return true; // New owner photo uploaded
        }
        
        // Check if new video is being uploaded
        if (ownerLiveVideo != null && !ownerLiveVideo.isEmpty()) {
            return true; // New video uploaded
        }
        
        // Check if existing room images have been removed
        Set<String> keptRoomImageUrls = new HashSet<>(existingRoomImageUrls == null ? List.of() : existingRoomImageUrls);
        if (!currentRoomImageUrls.equals(keptRoomImageUrls)) {
            return true; // Images removed or changed
        }
        
        // Check if video has been removed
        if (currentVideoUrl != null && !currentVideoUrl.equals(existingOwnerVideoUrl)) {
            return true; // Video removed or changed
        }
        
        return false; // No media changes detected
    }

    private List<String> parseExistingRoomImageUrls(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(objectMapper.readValue(json, String[].class))
                    .map(this::blankToNull)
                    .filter(value -> value != null)
                    .distinct()
                    .toList();
        } catch (Exception error) {
            throw new BadRequestException("Existing listing media could not be processed.");
        }
    }

    private void validateListingRequest(ListingUpsertRequest request) {
        if (request.getTotalCapacity() == null || request.getTotalCapacity() <= 0) {
            throw new BadRequestException("Total capacity must be greater than zero.");
        }
    }

    private void ensureOwnerPhotoPresent(User owner) {
        if (owner.getProfilePhotoUrl() == null || owner.getProfilePhotoUrl().isBlank()) {
            throw new BadRequestException("Owner photo is required for listing verification.");
        }
    }

    private void ensureVerifiedAccess(User user) {
        if (!user.isProfileComplete() || !user.isIdentityVerified()) {
            throw new BadRequestException("Complete your profile and identity verification before using listing features.");
        }
    }

    private Specification<Listing> hasLiveStatus() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), ListingStatus.LIVE);
    }

    private Specification<Listing> matchesSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), like),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("locality")), like),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("addressLineOne")), like)
            );
        };
    }

    private Specification<Listing> matchesLocation(String location) {
        return (root, query, criteriaBuilder) -> {
            if (location == null || location.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("locality")), "%" + location.trim().toLowerCase(Locale.ROOT) + "%");
        };
    }

    private Specification<Listing> matchesPriceBucket(String bucket) {
        return (root, query, criteriaBuilder) -> {
            if (bucket == null || bucket.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return switch (bucket.toLowerCase(Locale.ROOT)) {
                case "low" -> criteriaBuilder.lt(root.get("rentAmount"), BigDecimal.valueOf(7000));
                case "mid" -> criteriaBuilder.between(root.get("rentAmount"), BigDecimal.valueOf(7000), BigDecimal.valueOf(9000));
                case "high" -> criteriaBuilder.gt(root.get("rentAmount"), BigDecimal.valueOf(9000));
                default -> criteriaBuilder.conjunction();
            };
        };
    }

    private Specification<Listing> matchesRoomKind(String roomKind) {
        return (root, query, criteriaBuilder) -> {
            if (roomKind == null || roomKind.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("roomKind"), Enum.valueOf(com.stazy.backend.common.enums.RoomKind.class, roomKind.toUpperCase(Locale.ROOT)));
        };
    }

    private Specification<Listing> matchesGenderCategory(String genderCategory) {
        return (root, query, criteriaBuilder) -> {
            if (genderCategory == null || genderCategory.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("genderCategory"), Enum.valueOf(com.stazy.backend.common.enums.GenderCategory.class, genderCategory.toUpperCase(Locale.ROOT)));
        };
    }

    private Specification<Listing> matchesMinimumRating(BigDecimal minimumRating) {
        return (root, query, criteriaBuilder) -> minimumRating == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.greaterThanOrEqualTo(root.get("ratingAverage"), minimumRating);
    }

    private String buildLocationLabel(String address, String locality) {
        String cleanedAddress = blankToNull(address);
        String cleanedLocality = blankToNull(locality);
        if (cleanedAddress == null) {
            return cleanedLocality;
        }
        if (cleanedLocality == null) {
            return cleanedAddress;
        }
        if (cleanedAddress.toLowerCase(Locale.ROOT).contains(cleanedLocality.toLowerCase(Locale.ROOT))) {
            return cleanedAddress;
        }
        return cleanedAddress + ", " + cleanedLocality;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
