package com.stazy.backend.media.controller;

import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.media.dto.MediaUploadRequest;
import com.stazy.backend.media.dto.MediaUploadResponse;
import com.stazy.backend.media.service.MediaService;
import com.stazy.backend.security.StazyPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    public ApiResponse<MediaUploadResponse> upload(
            @AuthenticationPrincipal StazyPrincipal principal,
            @ModelAttribute MediaUploadRequest request
    ) {
        return ApiResponse.ok("Media uploaded successfully.", mediaService.uploadForCurrentUser(principal.getUserId(), request));
    }
}
