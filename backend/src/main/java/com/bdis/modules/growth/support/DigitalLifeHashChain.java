package com.bdis.modules.growth.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DigitalLifeHashChain {

    public static final String HASH_VERSION = "sha256-v1";

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final ObjectMapper objectMapper;

    public DigitalLifeHashChain(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CanonicalEvent> sort(List<CanonicalEvent> events) {
        return events.stream()
                .sorted(
                        Comparator.comparing(CanonicalEvent::eventTime)
                                .thenComparing(CanonicalEvent::eventType)
                                .thenComparing(CanonicalEvent::targetId))
                .toList();
    }

    public List<HashedEvent> build(List<CanonicalEvent> events) {
        List<HashedEvent> result = new ArrayList<>();
        String previousHash = null;
        int sequence = 1;
        for (CanonicalEvent event : sort(events)) {
            String canonicalData = canonicalData(event);
            String eventHash = sha256(canonicalData + (previousHash == null ? "" : previousHash));
            result.add(new HashedEvent(sequence, event, canonicalData, previousHash, eventHash));
            previousHash = eventHash;
            sequence++;
        }
        return result;
    }

    public ChainVerification verifyStored(List<HashedEvent> events) {
        String previousHash = null;
        for (int index = 0; index < events.size(); index++) {
            HashedEvent event = events.get(index);
            int expectedSequence = index + 1;
            if (event.sequence() != expectedSequence) {
                return new ChainVerification(false, expectedSequence, event.event().eventType());
            }
            if (!java.util.Objects.equals(previousHash, event.previousHash())) {
                return new ChainVerification(false, expectedSequence, event.event().eventType());
            }
            String expectedHash =
                    sha256(event.canonicalData() + (previousHash == null ? "" : previousHash));
            if (!expectedHash.equals(event.eventHash())) {
                return new ChainVerification(false, expectedSequence, event.event().eventType());
            }
            previousHash = event.eventHash();
        }
        return new ChainVerification(true, null, null);
    }

    public String canonicalData(CanonicalEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("actorId", event.actorId());
        data.put("actorName", event.actorName());
        data.put("eventTime", TIME_FORMATTER.format(event.eventTime()));
        data.put("eventType", event.eventType());
        data.put("payload", normalize(event.payload()));
        data.put("targetId", event.targetId());
        data.put("targetType", event.targetType());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("证据链事件规范化失败", exception);
        }
    }

    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前环境不支持 SHA-256", exception);
        }
    }

    private Object normalize(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof LocalDateTime time) {
            return TIME_FORMATTER.format(time);
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), normalize(item)));
            return sorted;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> normalized = new ArrayList<>();
            iterable.forEach(item -> normalized.add(normalize(item)));
            return normalized;
        }
        return String.valueOf(value);
    }

    public record CanonicalEvent(
            String targetType,
            Long targetId,
            String eventType,
            LocalDateTime eventTime,
            Long actorId,
            String actorName,
            Map<String, Object> payload) {}

    public record HashedEvent(
            int sequence,
            CanonicalEvent event,
            String canonicalData,
            String previousHash,
            String eventHash) {}

    public record ChainVerification(
            boolean verified, Integer failedSequence, String failedEventType) {}
}
