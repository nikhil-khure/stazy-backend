package com.stazy.backend.listing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stazy.backend.common.api.PageResponse;
import com.stazy.backend.common.enums.ListingStatus;
import com.stazy.backend.common.enums.MediaType;
import com.stazy.backend.common.enums.VerificationStatus;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.integration.cloudinary.CloudinaryService;
import com.stazy.backend.integration.cloudinary.UploadedAsset;
import com.stazy.backend.listing.dto.ListingMediaResponse;
import com.stazy.backend.listing.dto.ListingResponse;
import com.stazy.backend.listing.dto.ListingUpsertRequest;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.listing.entity.ListingMedia;
import com.stazy.backend.listing.repository.ListingMediaRepository;
import com.stazy.backend.listing.repository.ListingRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.service.CurrentUserService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    private final ObjectMapper objectMapper;

    public ListingService(
            ListingRepository listingRepository,
            ListingMediaRepository listingMediaRepository,
            CurrentUserService currentUserService,
            CloudinaryService cloudinaryService,
            ObjectMapper objectMapper
    ) {
        this.listingRepository = listingRepository;
        this.listingMediaRepository = listingMediaRepository;
        this.currentUserService = currentUserService;
        this.cloudinaryService = cloudinaryService;
        this.objectMapper = objectMapper;
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
        updateOwnerPhotoIfProvided(owner, request.getOwnerPhoto());
        ensureOwnerPhotoPresent(owner);

        Listing listing = new Listing();
        listing.setOwnerUser(owner);
        applyRequestToListing(listing, request);
        listing.setAvailableCapacity(request.getTotalCapacity());
        listing.setStatus(ListingStatus.UNDER_REVIEW);
        listing.setLatestFakeDetectionStatus(VerificationStatus.PENDING);
        Listing saved = listingRepository.save(listing);
        replaceMedia(saved, request.getRoomImages(), request.getOwnerLiveVideo(), ownerId);
        return mapListing(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"listing-search", "listing-detail"}, allEntries = true)
    public ListingResponse update(UUID ownerId, UUID listingId, ListingUpsertRequest request) {
        validateListingRequest(request);
        User owner = currentUserService.requireUser(ownerId);
        ensureVerifiedAccess(owner);
        updateOwnerPhotoIfProvided(owner, request.getOwnerPhoto());
        ensureOwnerPhotoPresent(owner);
        Listing listing = listingRepository.findByIdAndOwnerUser(listingId, owner)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        applyRequestToListing(listing, request);
        if (request.getRoomImages() != null || request.getOwnerLiveVideo() != null) {
            replaceMedia(listing, request.getRoomImages(), request.getOwnerLiveVideo(), ownerId);
        }
        return mapListing(listingRepository.save(listing));
    }

    @Transactional
    @CacheEvict(cacheNames = {"listing-search", "listing-detail"}, allEntries = true)
    public void delete(UUID ownerId, UUID listingId) {
        User owner = currentUserService.requireUser(ownerId);
        Listing listing = listingRepository.findByIdAndOwnerUser(listingId, owner)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        listingMediaRepository.deleteByListing(listing);
        listingRepository.delete(listing);
    }

    private void applyRequestToListing(Listing listing, ListingUpsertRequest request) {
        listing.setTitle(request.getTitle().trim());
        listing.setDescription(blankToNull(request.getDescription()));
        listing.setAddressLineOne(request.getLocation().trim());
        listing.setLocality(request.getLocation().trim());
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

    private void replaceMedia(Listing listing, MultipartFile[] roomImages, MultipartFile ownerLiveVideo, UUID ownerId) {
        listingMediaRepository.deleteByListing(listing);
        int sortOrder = 0;
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
                media.setPrimary(sortOrder == 1);
                media.setUploadedBy(currentUserService.requireUser(ownerId));
                listingMediaRepository.save(media);
            }
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
                listing.getLocality() != null ? listing.getLocality() : listing.getAddressLineOne(),
                listing.getRentAmount(),
                listing.getRatingAverage(),
                listing.getRatingCount() == null ? 0 : listing.getRatingCount(),
                listing.getRoomKind(),
                listing.getGenderCategory(),
                listing.getTotalCapacity(),
                listing.getAvailableCapacity(),
                listing.getStatus(),
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

    private void validateListingRequest(ListingUpsertRequest request) {
        if (request.getTotalCapacity() == null || request.getTotalCapacity() <= 0) {
            throw new BadRequestException("Total capacity must be greater than zero.");
        }
    }

    private void updateOwnerPhotoIfProvided(User owner, MultipartFile ownerPhoto) {
        if (ownerPhoto == null || ownerPhoto.isEmpty()) {
            return;
        }
        UploadedAsset uploadedAsset = cloudinaryService.uploadImage(ownerPhoto, "stazy/listings/owners/" + owner.getId());
        owner.setProfilePhotoUrl(uploadedAsset.url());
        owner.setProfilePhotoPublicId(uploadedAsset.publicId());
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
