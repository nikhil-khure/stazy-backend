package com.stazy.backend.media.service;

import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.integration.cloudinary.CloudinaryService;
import com.stazy.backend.integration.cloudinary.UploadedAsset;
import com.stazy.backend.media.dto.MediaUploadRequest;
import com.stazy.backend.media.dto.MediaUploadResponse;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.service.CurrentUserService;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    private final CurrentUserService currentUserService;
    private final CloudinaryService cloudinaryService;

    public MediaService(CurrentUserService currentUserService, CloudinaryService cloudinaryService) {
        this.currentUserService = currentUserService;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public MediaUploadResponse uploadForCurrentUser(java.util.UUID userId, MediaUploadRequest request) {
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required.");
        }

        User user = currentUserService.requireUser(userId);
        String folder = "stazy/uploads/" + user.getUserCode().toLowerCase(Locale.ROOT) + "/" + sanitizeUsage(request.getUsage());

        UploadedAsset uploadedAsset = uploadByType(file, folder);
        return new MediaUploadResponse(
                uploadedAsset.url(),
                uploadedAsset.publicId(),
                uploadedAsset.resourceType(),
                uploadedAsset.format(),
                uploadedAsset.bytes(),
                uploadedAsset.originalFilename()
        );
    }

    private UploadedAsset uploadByType(MultipartFile file, String folder) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return cloudinaryService.uploadImage(file, folder);
        }
        if (contentType != null && contentType.startsWith("video/")) {
            return cloudinaryService.uploadVideo(file, folder);
        }
        return cloudinaryService.uploadRaw(file, folder);
    }

    private String sanitizeUsage(String usage) {
        if (usage == null || usage.isBlank()) {
            return "general";
        }
        return usage.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-");
    }
}
