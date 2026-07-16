package com.bdis.modules.assistant.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AgentToolLogSanitizer {

    private static final int OUTPUT_JSON_LIMIT = 65_535;
    private static final Set<String> SENSITIVE_KEYS =
            Set.of("jwt", "token", "password", "apiKey", "secret", "rawResult", "vector");
    private static final Pattern WINDOWS_PATH = Pattern.compile("(?i)[a-z]:[\\\\/][^\\s\\\"']+");
    private static final Pattern UNIX_LOCAL_PATH =
            Pattern.compile("/(?:home|var|tmp|Users)/[^\\s\\\"']+");
    private static final Pattern FILE_URI = Pattern.compile("(?i)file:[^\\s\\\"']+");
    private static final Pattern JWT =
            Pattern.compile("(?i)\\beyJ[a-z0-9_-]+\\.[a-z0-9_-]+\\.[a-z0-9_-]+\\b");
    private static final Pattern OPENAI_STYLE_KEY = Pattern.compile("(?i)\\bsk-[a-z0-9_-]{16,}\\b");
    private static final Pattern LABELED_SECRET =
            Pattern.compile(
                    "(?i)\\b(authorization|api[_-]?key|token|password)\\b\\s*[:=]\\s*(?:bearer\\s+)?[^\\s,;]+");

    private final ObjectMapper objectMapper;

    public AgentToolLogSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String outputJson(Object value) {
        if (value == null) {
            return null;
        }
        JsonNode tree = objectMapper.valueToTree(value);
        sanitize(tree);
        try {
            return truncate(objectMapper.writeValueAsString(tree), OUTPUT_JSON_LIMIT);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    public String text(String value, int limit) {
        if (value == null) {
            return null;
        }
        String sanitized = FILE_URI.matcher(value).replaceAll("[已脱敏路径]");
        sanitized = WINDOWS_PATH.matcher(sanitized).replaceAll("[已脱敏路径]");
        sanitized = UNIX_LOCAL_PATH.matcher(sanitized).replaceAll("[已脱敏路径]");
        sanitized = JWT.matcher(sanitized).replaceAll("[已脱敏令牌]");
        sanitized = OPENAI_STYLE_KEY.matcher(sanitized).replaceAll("[已脱敏密钥]");
        sanitized = LABELED_SECRET.matcher(sanitized).replaceAll("$1=[已脱敏]");
        return truncate(sanitized, limit);
    }

    public String sha256(String value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    private void sanitize(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    fields.remove();
                } else if (field.getValue().isTextual()) {
                    objectNode.put(
                            field.getKey(), text(field.getValue().asText(), OUTPUT_JSON_LIMIT));
                } else {
                    sanitize(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::sanitize);
        }
    }

    private boolean isSensitive(String key) {
        return SENSITIVE_KEYS.stream().anyMatch(item -> item.equalsIgnoreCase(key));
    }

    private String truncate(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
