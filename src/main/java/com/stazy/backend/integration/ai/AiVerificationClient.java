package com.stazy.backend.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stazy.backend.common.config.AppProperties;
import com.stazy.backend.common.exception.BadRequestException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiVerificationClient {

    private static final List<String> LISTING_ENDPOINT_FALLBACK_PATHS = List.of(
            "/verify-listing",
            "/listing-verification",
            "/verify_listing",
            "/listing_verification"
    );

    private final AppProperties appProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public AiVerificationClient(AppProperties appProperties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public JsonNode verifyStudent(MultipartFile liveImage, MultipartFile idCardImage, String collegeName, String prn) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("live_image", asResource(liveImage));
        body.add("id_card_image", asResource(idCardImage));
        body.add("college_name", collegeName);
        body.add("prn", prn);
        return postMultipart(appProperties.getAi().getStudentVerificationUrl(), body);
    }

    public JsonNode verifyOwner(MultipartFile liveImage, MultipartFile panImage, MultipartFile signature, String ownerName, String panNumber) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("live_image", asResource(liveImage));
        body.add("pan_image", asResource(panImage));
        body.add("user_signature", asResource(signature));
        body.add("owner_name", ownerName);
        body.add("pan_number", panNumber);
        return postMultipart(appProperties.getAi().getOwnerVerificationUrl(), body);
    }

    public JsonNode verifyListing(MultipartFile ownerLiveVideo, MultipartFile ownerPhoto, List<MultipartFile> roomImages) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("owner_live_video", asResource(ownerLiveVideo));
        body.add("owner_photo", asResource(ownerPhoto));
        roomImages.forEach(image -> body.add("room_images", asResource(image)));
        return postMultipartWithFallback(appProperties.getAi().getListingVerificationUrl(), body, LISTING_ENDPOINT_FALLBACK_PATHS);
    }

    public JsonNode verifyListingFromUrls(String ownerLiveVideoUrl, String ownerPhotoUrl, List<String> roomImageUrls) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("owner_live_video", downloadAsResource(ownerLiveVideoUrl, "owner-live-video.mp4"));
        body.add("owner_photo", downloadAsResource(ownerPhotoUrl, "owner-photo.jpg"));
        roomImageUrls.forEach(url -> body.add("room_images", downloadAsResource(url, "room-image.jpg")));
        return postMultipartWithFallback(appProperties.getAi().getListingVerificationUrl(), body, LISTING_ENDPOINT_FALLBACK_PATHS);
    }

    private JsonNode postMultipart(String url, MultiValueMap<String, Object> body) {
        try {
            return executeMultipart(url, body);
        } catch (Exception ex) {
            throw new BadRequestException("AI verification request failed: " + ex.getMessage());
        }
    }

    private JsonNode postMultipartWithFallback(String url, MultiValueMap<String, Object> body, List<String> fallbackPaths) {
        RestClientResponseException lastNotFound = null;

        for (String candidateUrl : buildCandidateUrls(url, fallbackPaths)) {
            try {
                return executeMultipart(candidateUrl, body);
            } catch (RestClientResponseException ex) {
                if (ex.getRawStatusCode() == 404) {
                    lastNotFound = ex;
                    continue;
                }
                throw new BadRequestException("AI verification request failed: " + ex.getRawStatusCode() + " " + ex.getStatusText() + formatResponseBody(ex.getResponseBodyAsString()));
            } catch (Exception ex) {
                throw new BadRequestException("AI verification request failed: " + ex.getMessage());
            }
        }

        if (lastNotFound != null) {
            throw new BadRequestException("AI verification request failed: " + lastNotFound.getRawStatusCode() + " " + lastNotFound.getStatusText() + formatResponseBody(lastNotFound.getResponseBodyAsString()));
        }

        throw new BadRequestException("AI verification request failed: Listing verification endpoint is not reachable.");
    }

    private JsonNode executeMultipart(String url, MultiValueMap<String, Object> body) throws Exception {
        System.out.println("🔥 CALLING FLASK URL: " + url);
        System.out.println("🔥 BODY KEYS: " + body.keySet());
        RestClient client = restClientBuilder.build();
        String responseBody = client.post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(responseBody);
    }

    private Set<String> buildCandidateUrls(String url, List<String> fallbackPaths) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(url);

        try {
            URI uri = URI.create(url);
            String origin = uri.getScheme() + "://" + uri.getAuthority();
            String path = uri.getPath() == null ? "" : uri.getPath();
            int lastSlash = path.lastIndexOf('/');
            String parentPath = lastSlash > 0 ? path.substring(0, lastSlash) : "";

            for (String fallbackPath : fallbackPaths) {
                String normalizedPath = normalizePath(fallbackPath);
                candidates.add(origin + normalizedPath);
                if (!parentPath.isBlank() && !"/".equals(parentPath)) {
                    candidates.add(origin + parentPath + normalizedPath);
                }
            }
        } catch (Exception ignored) {
            // If the configured URL is malformed, keep the original candidate so the existing error surfaces.
        }

        return candidates;
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private String formatResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        return ": " + responseBody;
    }

    private ByteArrayResource asResource(MultipartFile file) {
        try {
            return new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (Exception ex) {
            throw new BadRequestException("Failed to read uploaded file: " + ex.getMessage());
        }
    }

    private ByteArrayResource downloadAsResource(String url, String fallbackFileName) {
        try {
            RestClient client = restClientBuilder.build();
            byte[] bytes = client.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(byte[].class);
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return fallbackFileName;
                }
            };
        } catch (Exception ex) {
            throw new BadRequestException("Failed to download file from Cloudinary for AI verification: " + ex.getMessage());
        }
    }
}
