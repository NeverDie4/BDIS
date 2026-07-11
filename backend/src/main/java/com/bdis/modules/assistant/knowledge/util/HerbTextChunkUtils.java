package com.bdis.modules.assistant.knowledge.util;

import com.bdis.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.util.StringUtils;

public final class HerbTextChunkUtils {

    private HerbTextChunkUtils() {}

    public static List<String> split(String content, int chunkSize, int overlap) {
        validateSettings(chunkSize, overlap);
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            if (end < normalized.length()) {
                int paragraphBreak = normalized.lastIndexOf("\n\n", end);
                if (paragraphBreak > start + chunkSize / 2) {
                    end = paragraphBreak;
                }
            }
            String chunk = normalized.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
            int nextStart = Math.max(0, end - overlap);
            start = nextStart > start ? nextStart : end;
            while (start < normalized.length()
                    && Character.isWhitespace(normalized.charAt(start))) {
                start++;
            }
        }
        return chunks;
    }

    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private static void validateSettings(int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new BusinessException("chunk-size 必须大于0");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new BusinessException("chunk-overlap 必须大于等于0且小于chunk-size");
        }
    }
}
