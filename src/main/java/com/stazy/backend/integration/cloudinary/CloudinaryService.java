package com.stazy.backend.integration.cloudinary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stazy.backend.common.config.AppProperties;
import com.stazy.backend.common.exception.BadRequestException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CloudinaryService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024L * 1024L;
    private static final long MAX_VIDEO_SIZE = 100L * 1024L * 1024L;

    private final AppProperties appProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public CloudinaryService(AppProperties appProperties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public UploadedAsset uploadImage(MultipartFile file, String folder) {
        validateFile(file, true);
        return upload(file, folder, "image");
    }

    public UploadedAsset uploadVideo(MultipartFile file, String folder) {
        validateFile(file, false);
        return upload(file, folder, "video");
    }

    public UploadedAsset uploadRaw(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required.");
        }
        return upload(file, folder, "raw");
    }

    public DownloadedAsset downloadFile(String publicId, String assetUrl) {
        if (assetUrl == null || assetUrl.isBlank()) {
            throw new BadRequestException("Resume file is not available.");
        }
        CloudinaryAssetReference assetReference = resolveAssetReference(publicId, assetUrl);
        try {
            RestClient client = restClientBuilder.build();
            ResponseEntity<byte[]> response = client.get()
                    .uri(buildSignedDownloadUrl(assetReference))
                    .retrieve()
                    .toEntity(byte[].class);
            return toDownloadedAsset(assetUrl, response);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to load resume file: " + ex.getRawStatusCode() + " " + ex.getStatusText());
        } catch (Exception ex) {
            throw new BadRequestException("Failed to load resume file: " + ex.getMessage());
        }
    }

    private UploadedAsset upload(MultipartFile file, String folder, String resourceType) {
        ensureConfigured();
        try {
            RestClient client = restClientBuilder
                    .baseUrl("https://api.cloudinary.com/v1_1/" + appProperties.getCloudinary().getCloudName())
                    .build();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", asResource(file));
            body.add("folder", folder);

            String uploadPreset = appProperties.getCloudinary().getUploadPreset();
            if (uploadPreset != null && !uploadPreset.isBlank()) {
                body.add("upload_preset", uploadPreset);
            } else {
                long timestamp = Instant.now().getEpochSecond();
                body.add("timestamp", String.valueOf(timestamp));
                body.add("api_key", appProperties.getCloudinary().getApiKey());
                body.add("signature", generateSignature(Map.of(
                        "folder", folder,
                        "timestamp", String.valueOf(timestamp)
                )));
            }

            ResponseEntity<String> response = client.post()
                    .uri("/" + resourceType + "/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return new UploadedAsset(
                    jsonNode.path("secure_url").asText(),
                    jsonNode.path("public_id").asText(),
                    jsonNode.path("resource_type").asText(),
                    jsonNode.path("format").asText(),
                    jsonNode.path("bytes").asLong(),
                    file.getOriginalFilename()
            );
        } catch (Exception ex) {
            throw new BadRequestException("Failed to upload file to Cloudinary: " + ex.getMessage());
        }
    }

    private void validateFile(MultipartFile file, boolean image) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required.");
        }
        if (image && (file.getContentType() == null || !file.getContentType().startsWith("image/"))) {
            throw new BadRequestException("Only image files are allowed.");
        }
        if (!image && (file.getContentType() == null || !file.getContentType().startsWith("video/"))) {
            throw new BadRequestException("Only video files are allowed.");
        }
        long limit = image ? MAX_IMAGE_SIZE : MAX_VIDEO_SIZE;
        if (file.getSize() > limit) {
            throw new BadRequestException("Uploaded file exceeds the allowed size limit.");
        }
    }

    private ByteArrayResource asResource(MultipartFile file) throws Exception {
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
    }

    private String generateSignature(Map<String, String> params) {
        try {
            List<String> pairs = new ArrayList<>();
            params.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEach(entry -> pairs.add(entry.getKey() + "=" + entry.getValue()));
            String payload = String.join("&", pairs) + appProperties.getCloudinary().getApiSecret();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate Cloudinary signature.", ex);
        }
    }

    private void ensureConfigured() {
        if (appProperties.getCloudinary().getCloudName() == null || appProperties.getCloudinary().getCloudName().isBlank()) {
            throw new BadRequestException("Cloudinary is not configured.");
        }
    }

    private DownloadedAsset toDownloadedAsset(String assetUrl, ResponseEntity<byte[]> response) {
        byte[] content = response.getBody();
        if (content == null || content.length == 0) {
            throw new BadRequestException("Resume file is empty.");
        }

        return new DownloadedAsset(
                content,
                resolveMediaType(assetUrl, response.getHeaders().getContentType()),
                resolveFileName(assetUrl)
        );
    }

    private String buildSignedDownloadUrl(CloudinaryAssetReference assetReference) {
        ensureConfigured();
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> signatureParams = new java.util.LinkedHashMap<>();
        signatureParams.put("format", assetReference.format());
        signatureParams.put("public_id", assetReference.publicId());
        signatureParams.put("timestamp", String.valueOf(timestamp));
        if (assetReference.deliveryType() != null && !assetReference.deliveryType().isBlank()) {
            signatureParams.put("type", assetReference.deliveryType());
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://api.cloudinary.com/v1_1/" + appProperties.getCloudinary().getCloudName() + "/" + assetReference.resourceType() + "/download")
                .queryParam("public_id", assetReference.publicId())
                .queryParam("format", assetReference.format())
                .queryParam("timestamp", timestamp)
                .queryParam("api_key", appProperties.getCloudinary().getApiKey())
                .queryParam("signature", generateSignature(signatureParams));

        if (assetReference.deliveryType() != null && !assetReference.deliveryType().isBlank()) {
            builder.queryParam("type", assetReference.deliveryType());
        }

        return builder.build(true).toUriString();
    }

    private CloudinaryAssetReference resolveAssetReference(String publicId, String assetUrl) {
        try {
            URI uri = URI.create(assetUrl);
            String[] segments = uri.getPath().split("/");
            String resourceType = segments.length > 2 && !segments[2].isBlank() ? segments[2] : "raw";
            String deliveryType = segments.length > 3 && !segments[3].isBlank() ? segments[3] : "upload";

            String resolvedPublicId = publicId == null || publicId.isBlank()
                    ? derivePublicIdFromUrl(assetUrl)
                    : publicId.trim();
            String format = deriveFormat(resolvedPublicId, assetUrl);
            if (format == null || format.isBlank()) {
                throw new BadRequestException("Could not determine resume file format.");
            }

            return new CloudinaryAssetReference(resourceType, deliveryType, resolvedPublicId, format);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Resume file URL is invalid.");
        }
    }

    private String derivePublicIdFromUrl(String assetUrl) {
        String sanitizedUrl = assetUrl.split("\\?")[0];
        int versionIndex = sanitizedUrl.indexOf("/v");
        if (versionIndex < 0) {
            int resourceTypeIndex = sanitizedUrl.indexOf("/raw/");
            if (resourceTypeIndex < 0) {
                return resolveFileName(assetUrl);
            }
            return sanitizedUrl.substring(resourceTypeIndex + 5);
        }
        int assetStart = sanitizedUrl.indexOf('/', versionIndex + 2);
        if (assetStart < 0 || assetStart + 1 >= sanitizedUrl.length()) {
            return resolveFileName(assetUrl);
        }
        return URLDecoder.decode(sanitizedUrl.substring(assetStart + 1), StandardCharsets.UTF_8);
    }

    private String deriveFormat(String publicId, String assetUrl) {
        String value = publicId;
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex + 1 < value.length()) {
            return value.substring(dotIndex + 1).toLowerCase();
        }
        String fileName = resolveFileName(assetUrl);
        int fileDotIndex = fileName.lastIndexOf('.');
        if (fileDotIndex >= 0 && fileDotIndex + 1 < fileName.length()) {
            return fileName.substring(fileDotIndex + 1).toLowerCase();
        }
        return null;
    }

    private MediaType resolveMediaType(String assetUrl, MediaType responseMediaType) {
        if (responseMediaType != null && !MediaType.APPLICATION_OCTET_STREAM.includes(responseMediaType)) {
            return responseMediaType;
        }
        String lowerCaseUrl = assetUrl.toLowerCase();
        if (lowerCaseUrl.contains(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lowerCaseUrl.contains(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        if (lowerCaseUrl.contains(".doc")) {
            return MediaType.parseMediaType("application/msword");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String resolveFileName(String assetUrl) {
        String sanitizedUrl = assetUrl.split("\\?")[0];
        int slashIndex = sanitizedUrl.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? sanitizedUrl.substring(slashIndex + 1) : sanitizedUrl;
        String decoded = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        if (decoded.isBlank()) {
            return sanitizedUrl.toLowerCase().contains(".pdf") ? "resume.pdf" : "resume";
        }
        return decoded;
    }

    private record CloudinaryAssetReference(String resourceType, String deliveryType, String publicId, String format) {
    }
}
