package com.bdis.modules.spectrum.client;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.spectrum.config.HerbRecognitionProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class HerbFeatureVectorClientImpl implements HerbFeatureVectorClient {

    private final HerbRecognitionProperties properties;
    private final Path uploadRoot;

    public HerbFeatureVectorClientImpl(
            HerbRecognitionProperties properties,
            @Value("${file.upload-path:uploads}") String uploadPath) {
        this.properties = properties;
        this.uploadRoot = Path.of(uploadPath).normalize();
    }

    @Override
    public List<Double> extract(HerbImageEntity image) {
        if (!properties.isEnabled() || properties.isMockEnabled()) {
            return Collections.emptyList();
        }
        Path imagePath = resolveImagePath(image.getImageUrl());
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imagePath));
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        org.springframework.http.HttpEntity<MultiValueMap<String, Object>> request =
                new org.springframework.http.HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response =
                    restTemplate().postForEntity(properties.getFeatureServiceUrl(), request, Map.class);
            return parseVector(response.getBody());
        } catch (RuntimeException exception) {
            throw new BusinessException("Feature service call failed: " + exception.getMessage());
        }
    }

    private Path resolveImagePath(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException("Image URL is required");
        }
        String relativePath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
        if (relativePath.startsWith("uploads/")) {
            relativePath = relativePath.substring("uploads/".length());
        }
        Path imagePath = uploadRoot.resolve(relativePath).normalize();
        if (!imagePath.startsWith(uploadRoot) || !Files.isRegularFile(imagePath)) {
            throw new BusinessException("Image file not found");
        }
        return imagePath;
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return new RestTemplate(requestFactory);
    }

    @SuppressWarnings("unchecked")
    private List<Double> parseVector(Map<String, Object> body) {
        if (body == null || !(body.get("data") instanceof Map<?, ?> data)) {
            throw new BusinessException("Feature service returned empty response");
        }
        Object vectorValue = data.get("vector");
        if (!(vectorValue instanceof List<?> values)) {
            throw new BusinessException("Feature service returned invalid vector");
        }
        return values.stream().map(value -> ((Number) value).doubleValue()).toList();
    }
}
