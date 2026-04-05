package com.stazy.backend.verification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.enums.VerificationAttachmentType;
import com.stazy.backend.common.enums.VerificationStatus;
import com.stazy.backend.common.enums.VerificationType;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.integration.ai.AiVerificationClient;
import com.stazy.backend.integration.cloudinary.CloudinaryService;
import com.stazy.backend.integration.cloudinary.UploadedAsset;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.listing.entity.ListingMedia;
import com.stazy.backend.listing.repository.ListingMediaRepository;
import com.stazy.backend.listing.repository.ListingRepository;
import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.profile.repository.OwnerProfileRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.CurrentUserService;
import com.stazy.backend.verification.dto.OwnerVerificationRequest;
import com.stazy.backend.verification.dto.StudentVerificationRequest;
import com.stazy.backend.verification.dto.VerificationResultResponse;
import com.stazy.backend.verification.entity.VerificationAttachment;
import com.stazy.backend.verification.entity.VerificationRequest;
import com.stazy.backend.verification.repository.VerificationAttachmentRepository;
import com.stazy.backend.verification.repository.VerificationRequestRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final ListingRepository listingRepository;
    private final ListingMediaRepository listingMediaRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationAttachmentRepository verificationAttachmentRepository;
    private final CloudinaryService cloudinaryService;
    private final AiVerificationClient aiVerificationClient;
    private final ObjectMapper objectMapper;

    public VerificationService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            OwnerProfileRepository ownerProfileRepository,
            ListingRepository listingRepository,
            ListingMediaRepository listingMediaRepository,
            VerificationRequestRepository verificationRequestRepository,
            VerificationAttachmentRepository verificationAttachmentRepository,
            CloudinaryService cloudinaryService,
            AiVerificationClient aiVerificationClient,
            ObjectMapper objectMapper
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.listingRepository = listingRepository;
        this.listingMediaRepository = listingMediaRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.verificationAttachmentRepository = verificationAttachmentRepository;
        this.cloudinaryService = cloudinaryService;
        this.aiVerificationClient = aiVerificationClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VerificationResultResponse verifyStudent(UUID userId, StudentVerificationRequest request) {
        User user = currentUserService.requireUser(userId);
        if (user.getPrimaryRoleCode() != RoleName.STUDENT) {
            throw new BadRequestException("Only students can use student verification.");
        }
        ensureProfileCompleted(user);
        if (request.getLiveImage() == null || request.getIdCardImage() == null) {
            throw new BadRequestException("Live image and ID card image are required.");
        }

        VerificationRequest verificationRequest = new VerificationRequest();
        verificationRequest.setUser(user);
        verificationRequest.setRequestedByUser(user);
        verificationRequest.setVerificationType(VerificationType.STUDENT_IDENTITY);
        verificationRequest.setStatus(VerificationStatus.PROCESSING);
        verificationRequest.setAiEndpoint("student-verification");
        verificationRequest.setRequestSummaryJson(buildStudentSummary(request));
        verificationRequest = verificationRequestRepository.save(verificationRequest);

        attachUploadedFile(verificationRequest, VerificationAttachmentType.LIVE_IMAGE, cloudinaryService.uploadImage(request.getLiveImage(), "stazy/verifications/student/" + user.getId()), request.getLiveImage().getContentType(), request.getLiveImage().getSize(), 0);
        attachUploadedFile(verificationRequest, VerificationAttachmentType.ID_CARD_IMAGE, cloudinaryService.uploadImage(request.getIdCardImage(), "stazy/verifications/student/" + user.getId()), request.getIdCardImage().getContentType(), request.getIdCardImage().getSize(), 1);

        JsonNode aiResponse = aiVerificationClient.verifyStudent(request.getLiveImage(), request.getIdCardImage(), request.getCollegeName(), request.getPrn());
        return finalizeUserVerification(user, verificationRequest, aiResponse);
    }

    @Transactional
    public VerificationResultResponse verifyOwner(UUID userId, OwnerVerificationRequest request) {
        User user = currentUserService.requireUser(userId);
        if (user.getPrimaryRoleCode() != RoleName.OWNER) {
            throw new BadRequestException("Only owners can use owner verification.");
        }
        ensureProfileCompleted(user);
        if (request.getLiveImage() == null || request.getPanImage() == null || request.getUserSignature() == null) {
            throw new BadRequestException("Live image, PAN image, and signature are required.");
        }

        VerificationRequest verificationRequest = new VerificationRequest();
        verificationRequest.setUser(user);
        verificationRequest.setRequestedByUser(user);
        verificationRequest.setVerificationType(VerificationType.OWNER_IDENTITY);
        verificationRequest.setStatus(VerificationStatus.PROCESSING);
        verificationRequest.setAiEndpoint("owner-verification");
        verificationRequest.setRequestSummaryJson(buildOwnerSummary(request));
        verificationRequest = verificationRequestRepository.save(verificationRequest);

        attachUploadedFile(verificationRequest, VerificationAttachmentType.LIVE_IMAGE, cloudinaryService.uploadImage(request.getLiveImage(), "stazy/verifications/owner/" + user.getId()), request.getLiveImage().getContentType(), request.getLiveImage().getSize(), 0);
        attachUploadedFile(verificationRequest, VerificationAttachmentType.PAN_IMAGE, cloudinaryService.uploadImage(request.getPanImage(), "stazy/verifications/owner/" + user.getId()), request.getPanImage().getContentType(), request.getPanImage().getSize(), 1);
        attachUploadedFile(verificationRequest, VerificationAttachmentType.SIGNATURE, cloudinaryService.uploadImage(request.getUserSignature(), "stazy/verifications/owner/" + user.getId()), request.getUserSignature().getContentType(), request.getUserSignature().getSize(), 2);

        JsonNode aiResponse = aiVerificationClient.verifyOwner(request.getLiveImage(), request.getPanImage(), request.getUserSignature(), request.getOwnerName(), request.getPanNumber());
        OwnerProfile ownerProfile = ownerProfileRepository.findByUser(user).orElse(null);
        if (ownerProfile != null) {
            ownerProfile.setPanNumber(request.getPanNumber());
        }
        return finalizeUserVerification(user, verificationRequest, aiResponse);
    }

    @Transactional
    public VerificationResultResponse verifyListing(UUID reviewerId, UUID listingId) {
        User reviewer = currentUserService.requireUser(reviewerId);
        if (reviewer.getPrimaryRoleCode() != RoleName.ADMIN && reviewer.getPrimaryRoleCode() != RoleName.SUPER_ADMIN) {
            throw new BadRequestException("Only admins can verify listings.");
        }
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
        List<ListingMedia> media = listingMediaRepository.findByListingOrderBySortOrderAsc(listing);
        String ownerLiveVideoUrl = media.stream()
                .filter(item -> item.getMediaType() == com.stazy.backend.common.enums.MediaType.VIDEO)
                .map(ListingMedia::getUrl)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Listing does not have an owner live video."));
        List<String> roomImageUrls = media.stream()
                .filter(item -> item.getMediaType() == com.stazy.backend.common.enums.MediaType.IMAGE)
                .map(ListingMedia::getUrl)
                .toList();
        if (roomImageUrls.isEmpty()) {
            throw new BadRequestException("Listing does not have room images.");
        }
        String ownerPhotoUrl = listing.getOwnerUser().getProfilePhotoUrl();
        if (ownerPhotoUrl == null || ownerPhotoUrl.isBlank()) {
            throw new BadRequestException("Owner profile photo is required before listing verification.");
        }

        VerificationRequest verificationRequest = new VerificationRequest();
        verificationRequest.setListing(listing);
        verificationRequest.setRequestedByUser(reviewer);
        verificationRequest.setUser(listing.getOwnerUser());
        verificationRequest.setVerificationType(VerificationType.LISTING_FAKE_DETECTION);
        verificationRequest.setStatus(VerificationStatus.PROCESSING);
        verificationRequest.setAiEndpoint("verify-listing");
        verificationRequest = verificationRequestRepository.save(verificationRequest);

        JsonNode aiResponse = aiVerificationClient.verifyListingFromUrls(ownerLiveVideoUrl, ownerPhotoUrl, roomImageUrls);
        boolean verified = extractVerified(aiResponse);
        verificationRequest.setVerified(verified);
        verificationRequest.setStatus(verified ? VerificationStatus.SUCCESS : VerificationStatus.FAILED);
        verificationRequest.setRawResponseJson(aiResponse);
        verificationRequest.setAcceptedParametersJson(extractNode(aiResponse, "accepted_parameters", "accepted", "accepted_params"));
        verificationRequest.setRejectedParametersJson(extractNode(aiResponse, "rejected_parameters", "rejected", "rejected_params"));
        verificationRequest.setFailureReason(extractMessage(aiResponse));
        verificationRequestRepository.save(verificationRequest);

        listing.setLatestVerificationRequest(verificationRequest);
        listing.setLatestFakeDetectionStatus(verificationRequest.getStatus());
        if (!verified) {
            listing.setStatus(com.stazy.backend.common.enums.ListingStatus.REJECTED);
            listing.setRejectionReason(extractMessage(aiResponse));
        }
        listingRepository.save(listing);

        return new VerificationResultResponse(
                verificationRequest.getId(),
                verificationRequest.getVerificationType(),
                verificationRequest.getStatus(),
                verified,
                extractMessage(aiResponse),
                verificationRequest.getAcceptedParametersJson(),
                verificationRequest.getRejectedParametersJson(),
                aiResponse,
                verificationRequest.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<VerificationResultResponse> historyForCurrentUser(UUID userId) {
        User user = currentUserService.requireUser(userId);
        return verificationRequestRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::map)
                .toList();
    }

    private VerificationResultResponse finalizeUserVerification(User user, VerificationRequest verificationRequest, JsonNode aiResponse) {
        boolean verified = extractVerified(aiResponse);
        verificationRequest.setVerified(verified);
        verificationRequest.setStatus(verified ? VerificationStatus.SUCCESS : VerificationStatus.FAILED);
        verificationRequest.setRawResponseJson(aiResponse);
        verificationRequest.setAcceptedParametersJson(extractNode(aiResponse, "accepted_parameters", "accepted", "accepted_params"));
        verificationRequest.setRejectedParametersJson(extractNode(aiResponse, "rejected_parameters", "rejected", "rejected_params"));
        verificationRequest.setFailureReason(extractMessage(aiResponse));
        verificationRequestRepository.save(verificationRequest);

        if (verified) {
            user.setIdentityVerified(true);
            userRepository.save(user);
        }

        return map(verificationRequest);
    }

    private void ensureProfileCompleted(User user) {
        if (!user.isProfileComplete()) {
            throw new BadRequestException("Profile must be 100% complete before verification.");
        }
    }

    private void attachUploadedFile(
            VerificationRequest request,
            VerificationAttachmentType attachmentType,
            UploadedAsset uploadedAsset,
            String mimeType,
            long size,
            int sortOrder
    ) {
        VerificationAttachment attachment = new VerificationAttachment();
        attachment.setVerificationRequest(request);
        attachment.setAttachmentType(attachmentType);
        attachment.setUrl(uploadedAsset.url());
        attachment.setPublicId(uploadedAsset.publicId());
        attachment.setMimeType(mimeType);
        attachment.setFileSizeBytes(size);
        attachment.setSortOrder(sortOrder);
        verificationAttachmentRepository.save(attachment);
    }

    private VerificationResultResponse map(VerificationRequest request) {
        return new VerificationResultResponse(
                request.getId(),
                request.getVerificationType(),
                request.getStatus(),
                request.isVerified(),
                request.getFailureReason(),
                request.getAcceptedParametersJson(),
                request.getRejectedParametersJson(),
                request.getRawResponseJson(),
                request.getCreatedAt()
        );
    }

    private boolean extractVerified(JsonNode response) {
        JsonNode verifiedNode = extractNode(response, "verified", "is_verified", "success");
        return verifiedNode != null && (
                (verifiedNode.isBoolean() && verifiedNode.booleanValue())
                        || (verifiedNode.isTextual() && List.of("true", "1", "yes", "verified", "success").contains(verifiedNode.asText().trim().toLowerCase()))
                        || (verifiedNode.isIntegralNumber() && verifiedNode.asInt() == 1)
        );
    }

    private String extractMessage(JsonNode response) {
        JsonNode messageNode = extractNode(response, "message", "reason", "details");
        return messageNode == null || messageNode.isNull() ? null : messageNode.asText();
    }

    private JsonNode extractNode(JsonNode response, String... names) {
        if (response == null || response.isNull()) {
            return null;
        }
        for (String name : names) {
            if (response.has(name)) {
                return response.get(name);
            }
        }
        if (response.isObject()) {
            var fields = response.fields();
            while (fields.hasNext()) {
                JsonNode nested = extractNode(fields.next().getValue(), names);
                if (nested != null) {
                    return nested;
                }
            }
        }
        if (response.isArray()) {
            for (JsonNode item : response) {
                JsonNode nested = extractNode(item, names);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private JsonNode buildStudentSummary(StudentVerificationRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("collegeName", request.getCollegeName());
        summary.put("prn", request.getPrn());
        summary.put("liveImageName", request.getLiveImage() == null ? null : request.getLiveImage().getOriginalFilename());
        summary.put("idCardImageName", request.getIdCardImage() == null ? null : request.getIdCardImage().getOriginalFilename());
        return objectMapper.valueToTree(summary);
    }

    private JsonNode buildOwnerSummary(OwnerVerificationRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ownerName", request.getOwnerName());
        summary.put("panNumber", request.getPanNumber());
        summary.put("liveImageName", request.getLiveImage() == null ? null : request.getLiveImage().getOriginalFilename());
        summary.put("panImageName", request.getPanImage() == null ? null : request.getPanImage().getOriginalFilename());
        summary.put("signatureName", request.getUserSignature() == null ? null : request.getUserSignature().getOriginalFilename());
        return objectMapper.valueToTree(summary);
    }
}
