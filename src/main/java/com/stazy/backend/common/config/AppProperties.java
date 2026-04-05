package com.stazy.backend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Otp otp = new Otp();
    private final Cloudinary cloudinary = new Cloudinary();
    private final Ai ai = new Ai();

    @Data
    public static class Jwt {
        private String issuer;
        private long accessTokenExpiryMinutes;
        private long refreshTokenExpiryDays;
        private String secret;
    }

    @Data
    public static class Cors {
        private String allowedOrigins;
    }

    @Data
    public static class Otp {
        private int expiryMinutes;
        private int maxAttempts;
        private boolean revealInResponse;
    }

    @Data
    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String uploadPreset;
        private String secureUrlPrefix;
    }

    @Data
    public static class Ai {
        private String studentVerificationUrl;
        private String ownerVerificationUrl;
        private String listingVerificationUrl;
    }
}
