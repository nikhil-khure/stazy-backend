package com.stazy.backend.complaint.service;

import com.stazy.backend.booking.entity.ActiveStay;
import com.stazy.backend.booking.repository.ActiveStayRepository;
import com.stazy.backend.common.enums.ComplaintMessageType;
import com.stazy.backend.common.enums.ComplaintStatus;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.complaint.dto.ComplaintActionRequest;
import com.stazy.backend.complaint.dto.ComplaintAttachmentResponse;
import com.stazy.backend.complaint.dto.ComplaintCreateRequest;
import com.stazy.backend.complaint.dto.ComplaintMessageResponse;
import com.stazy.backend.complaint.dto.ComplaintResponse;
import com.stazy.backend.complaint.entity.Complaint;
import com.stazy.backend.complaint.entity.ComplaintAttachment;
import com.stazy.backend.complaint.entity.ComplaintMessage;
import com.stazy.backend.complaint.repository.ComplaintAttachmentRepository;
import com.stazy.backend.complaint.repository.ComplaintMessageRepository;
import com.stazy.backend.complaint.repository.ComplaintRepository;
import com.stazy.backend.integration.cloudinary.CloudinaryService;
import com.stazy.backend.integration.cloudinary.UploadedAsset;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.listing.repository.ListingRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintMessageRepository complaintMessageRepository;
    private final ComplaintAttachmentRepository complaintAttachmentRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ActiveStayRepository activeStayRepository;
    private final CloudinaryService cloudinaryService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintMessageRepository complaintMessageRepository,
            ComplaintAttachmentRepository complaintAttachmentRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            ListingRepository listingRepository,
            ActiveStayRepository activeStayRepository,
            CloudinaryService cloudinaryService
    ) {
        this.complaintRepository = complaintRepository;
        this.complaintMessageRepository = complaintMessageRepository;
        this.complaintAttachmentRepository = complaintAttachmentRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.activeStayRepository = activeStayRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public ComplaintResponse createComplaint(UUID userId, ComplaintCreateRequest request) {
        User complainant = currentUserService.requireUser(userId);
        User againstUser = userRepository.findByUserCodeIgnoreCase(request.getAgainstUserCode().trim())
                .orElseThrow(() -> new NotFoundException("Target user not found."));
        if (complainant.getId().equals(againstUser.getId())) {
            throw new BadRequestException("You cannot create a complaint against yourself.");
        }
        if (complainant.getPrimaryRoleCode() == againstUser.getPrimaryRoleCode()) {
            throw new BadRequestException("Complaints are only supported between student and owner accounts.");
        }

        Listing relatedListing = resolveListing(request.getRelatedListingId());
        ActiveStay relatedStay = resolveStay(request.getRelatedStayId());
        validateRelationships(complainant, againstUser, relatedListing, relatedStay);

        Complaint complaint = new Complaint();
        complaint.setComplainantUser(complainant);
        complaint.setAgainstUser(againstUser);
        complaint.setRelatedListing(relatedListing);
        complaint.setRelatedStay(relatedStay);
        complaint.setTitle(request.getTitle().trim());
        complaint.setDescription(request.getDescription().trim());
        complaint.setCreatedByRoleCode(complainant.getPrimaryRoleCode());
        complaint.setAgainstRoleCode(againstUser.getPrimaryRoleCode());
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint = complaintRepository.save(complaint);

        ComplaintMessage message = saveMessage(complaint, complainant, ComplaintMessageType.COMPLAINT, request.getDescription().trim());
        attachFiles(complaint, message, request.getAttachments());
        return map(complaintRepository.findById(complaint.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> filedComplaints(UUID userId) {
        User user = currentUserService.requireUser(userId);
        return complaintRepository.findByComplainantUserOrderByCreatedAtDesc(user).stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> receivedComplaints(UUID userId) {
        User user = currentUserService.requireUser(userId);
        return complaintRepository.findByAgainstUserOrderByCreatedAtDesc(user).stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaint(UUID userId, UUID complaintId) {
        Complaint complaint = requireComplaintForParticipant(userId, complaintId);
        return map(complaint);
    }

    @Transactional
    public ComplaintResponse submitJustification(UUID userId, UUID complaintId, ComplaintActionRequest request) {
        User actor = currentUserService.requireUser(userId);
        Complaint complaint = complaintRepository.findByIdAndAgainstUser(complaintId, actor)
                .orElseThrow(() -> new NotFoundException("Complaint not found."));
        if (complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new BadRequestException("This complaint is already closed.");
        }
        ComplaintMessage message = saveMessage(complaint, actor, ComplaintMessageType.JUSTIFICATION, request.getMessage().trim());
        attachFiles(complaint, message, request.getAttachments());
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setCurrentResolutionSummary(request.getMessage().trim());
        complaint.setClosedAt(null);
        return map(complaintRepository.save(complaint));
    }

    @Transactional
    public ComplaintResponse reComplaint(UUID userId, UUID complaintId, ComplaintActionRequest request) {
        User actor = currentUserService.requireUser(userId);
        Complaint complaint = complaintRepository.findByIdAndComplainantUser(complaintId, actor)
                .orElseThrow(() -> new NotFoundException("Complaint not found."));
        if (complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new BadRequestException("Closed complaints cannot be reopened.");
        }
        ComplaintMessage message = saveMessage(complaint, actor, ComplaintMessageType.RE_COMPLAINT, request.getMessage().trim());
        attachFiles(complaint, message, request.getAttachments());
        complaint.setStatus(ComplaintStatus.UNDER_PROGRESS);
        complaint.setCurrentResolutionSummary(request.getMessage().trim());
        complaint.setClosedAt(null);
        return map(complaintRepository.save(complaint));
    }

    @Transactional
    public ComplaintResponse closeComplaint(UUID userId, UUID complaintId) {
        User actor = currentUserService.requireUser(userId);
        Complaint complaint = complaintRepository.findByIdAndComplainantUser(complaintId, actor)
                .orElseThrow(() -> new NotFoundException("Complaint not found."));
        complaint.setStatus(ComplaintStatus.CLOSED);
        complaint.setClosedAt(OffsetDateTime.now());
        if (complaint.getCurrentResolutionSummary() == null || complaint.getCurrentResolutionSummary().isBlank()) {
            complaint.setCurrentResolutionSummary("Complaint closed by complainant.");
        }
        saveMessage(complaint, actor, ComplaintMessageType.RESOLUTION, "Complaint closed by complainant.");
        return map(complaintRepository.save(complaint));
    }

    private Listing resolveListing(UUID listingId) {
        if (listingId == null) {
            return null;
        }
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found."));
    }

    private ActiveStay resolveStay(UUID stayId) {
        if (stayId == null) {
            return null;
        }
        return activeStayRepository.findById(stayId)
                .orElseThrow(() -> new NotFoundException("Active stay not found."));
    }

    private void validateRelationships(User complainant, User againstUser, Listing relatedListing, ActiveStay relatedStay) {
        if (relatedListing != null && !relatedListing.getOwnerUser().getId().equals(againstUser.getId()) && !relatedListing.getOwnerUser().getId().equals(complainant.getId())) {
            throw new BadRequestException("The selected listing is not related to these users.");
        }
        if (relatedStay != null) {
            boolean match = (relatedStay.getStudentUser().getId().equals(complainant.getId()) && relatedStay.getOwnerUser().getId().equals(againstUser.getId()))
                    || (relatedStay.getStudentUser().getId().equals(againstUser.getId()) && relatedStay.getOwnerUser().getId().equals(complainant.getId()));
            if (!match) {
                throw new BadRequestException("The selected stay is not related to these users.");
            }
        }
    }

    private Complaint requireComplaintForParticipant(UUID userId, UUID complaintId) {
        User user = currentUserService.requireUser(userId);
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("Complaint not found."));
        boolean canAccess = complaint.getComplainantUser().getId().equals(user.getId())
                || complaint.getAgainstUser().getId().equals(user.getId())
                || user.getPrimaryRoleCode() == RoleName.ADMIN
                || user.getPrimaryRoleCode() == RoleName.SUPER_ADMIN;
        if (!canAccess) {
            throw new BadRequestException("You are not allowed to access this complaint.");
        }
        return complaint;
    }

    private ComplaintMessage saveMessage(Complaint complaint, User author, ComplaintMessageType type, String messageText) {
        ComplaintMessage message = new ComplaintMessage();
        message.setComplaint(complaint);
        message.setAuthorUser(author);
        message.setMessageType(type);
        message.setMessage(messageText);
        return complaintMessageRepository.save(message);
    }

    private void attachFiles(Complaint complaint, ComplaintMessage message, MultipartFile[] files) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            UploadedAsset uploadedAsset = uploadAttachment(file, complaint.getId());
            ComplaintAttachment attachment = new ComplaintAttachment();
            attachment.setComplaint(complaint);
            attachment.setComplaintMessage(message);
            attachment.setUrl(uploadedAsset.url());
            attachment.setPublicId(uploadedAsset.publicId());
            attachment.setMimeType(file.getContentType());
            attachment.setFileSizeBytes(file.getSize());
            complaintAttachmentRepository.save(attachment);
        }
    }

    private UploadedAsset uploadAttachment(MultipartFile file, UUID complaintId) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String folder = "stazy/complaints/" + complaintId;
        if (contentType.startsWith("image/")) {
            return cloudinaryService.uploadImage(file, folder);
        }
        if (contentType.startsWith("video/")) {
            return cloudinaryService.uploadVideo(file, folder);
        }
        return cloudinaryService.uploadRaw(file, folder);
    }

    private ComplaintResponse map(Complaint complaint) {
        List<ComplaintMessageResponse> messages = complaintMessageRepository.findByComplaintOrderByCreatedAtAsc(complaint).stream()
                .map(message -> new ComplaintMessageResponse(
                        message.getId(),
                        message.getAuthorUser().getUserCode(),
                        message.getAuthorUser().getDisplayName(),
                        message.getMessageType(),
                        message.getMessage(),
                        message.getCreatedAt(),
                        complaintAttachmentRepository.findByComplaintMessageOrderByCreatedAtAsc(message).stream()
                                .map(this::mapAttachment)
                                .toList()
                ))
                .toList();

        return new ComplaintResponse(
                complaint.getId(),
                complaint.getTitle(),
                complaint.getDescription(),
                complaint.getStatus(),
                complaint.getCurrentResolutionSummary(),
                complaint.getCreatedByRoleCode(),
                complaint.getAgainstRoleCode(),
                complaint.getComplainantUser().getUserCode(),
                complaint.getComplainantUser().getDisplayName(),
                complaint.getAgainstUser().getUserCode(),
                complaint.getAgainstUser().getDisplayName(),
                complaint.getRelatedListing() == null ? null : complaint.getRelatedListing().getId(),
                complaint.getRelatedStay() == null ? null : complaint.getRelatedStay().getId(),
                complaint.getClosedAt(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                messages
        );
    }

    private ComplaintAttachmentResponse mapAttachment(ComplaintAttachment attachment) {
        return new ComplaintAttachmentResponse(
                attachment.getId(),
                attachment.getUrl(),
                attachment.getMimeType(),
                attachment.getFileSizeBytes(),
                attachment.getCreatedAt()
        );
    }
}
