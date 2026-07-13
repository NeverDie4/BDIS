package com.bdis.common.security;

import com.bdis.common.exception.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JwtProperties properties;

    private final ObjectMapper objectMapper;

    public JwtUtils(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public IssuedToken generate(CurrentUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.getAccessTokenTtlMinutes() * 60);
        String jti = UUID.randomUUID().toString();
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", properties.getIssuer());
        payload.put("sub", String.valueOf(user.getUserId()));
        payload.put("username", user.getUsername());
        payload.put("jti", jti);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        String accessToken = unsignedToken + "." + sign(unsignedToken);
        return new IssuedToken(accessToken, jti, issuedAt, expiresAt);
    }

    public JwtClaims parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedException("Token 格式错误");
        }
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Token 签名无效");
        }
        Map<String, Object> payload = decodeJson(parts[1]);
        String issuer = String.valueOf(payload.get("iss"));
        if (!properties.getIssuer().equals(issuer)) {
            throw new UnauthorizedException("Token 签发方无效");
        }
        Instant expiresAt = Instant.ofEpochSecond(numberValue(payload.get("exp")));
        if (Instant.now().isAfter(expiresAt)) {
            throw new UnauthorizedException("Token 已过期");
        }
        return new JwtClaims(
                numberValue(payload.get("sub")),
                String.valueOf(payload.get("username")),
                String.valueOf(payload.get("jti")),
                Instant.ofEpochSecond(numberValue(payload.get("iat"))),
                expiresAt);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JWT 序列化失败", exception);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (IllegalArgumentException | IOException exception) {
            throw new UnauthorizedException("Token 内容无效");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(
                    new SecretKeySpec(
                            properties.getSecret().getBytes(StandardCharsets.UTF_8),
                            HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("JWT 签名失败", exception);
        }
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("Token 内容无效");
        }
    }
}
