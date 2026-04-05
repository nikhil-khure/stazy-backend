package com.stazy.backend.security;

import com.stazy.backend.common.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generateAccessToken(StazyPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getJwt().getAccessTokenExpiryMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(appProperties.getJwt().getIssuer())
                .subject(principal.getUsername())
                .claim("uid", principal.getUserId().toString())
                .claim("userCode", principal.getUserCode())
                .claim("roles", principal.getAuthorities().stream().map(Object::toString).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(StazyPrincipal principal, UUID tokenId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getJwt().getRefreshTokenExpiryDays(), ChronoUnit.DAYS);
        return Jwts.builder()
                .issuer(appProperties.getJwt().getIssuer())
                .subject(principal.getUsername())
                .id(tokenId.toString())
                .claims(Map.of(
                        "uid", principal.getUserId().toString(),
                        "userCode", principal.getUserCode(),
                        "type", "refresh"
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    public UUID extractTokenId(String token) {
        Claims claims = extractClaims(token);
        return claims.getId() == null ? null : UUID.fromString(claims.getId());
    }

    public boolean isValid(String token, String username) {
        Claims claims = extractClaims(token);
        return username.equalsIgnoreCase(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(appProperties.getJwt().getSecret().getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
