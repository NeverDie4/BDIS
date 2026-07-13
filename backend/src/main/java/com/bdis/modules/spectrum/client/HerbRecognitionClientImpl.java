package com.bdis.modules.spectrum.client;

import com.bdis.common.exception.BusinessException;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.spectrum.config.HerbRecognitionProperties;
import com.bdis.modules.spectrum.vo.HerbRecognitionCandidateVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class HerbRecognitionClientImpl implements HerbRecognitionClient {

    private final HerbRecognitionProperties properties;
    private final ObjectMapper objectMapper;
    private final FileResourceService fileResourceService;

    public HerbRecognitionClientImpl(
            HerbRecognitionProperties properties,
            ObjectMapper objectMapper,
            FileResourceService fileResourceService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fileResourceService = fileResourceService;
    }

    @Override
    public HerbRecognitionClientResponse recognize(HerbImageEntity image) {
        if (!properties.isEnabled() || properties.isMockEnabled()) {
            return mockResponse();
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
                    restTemplate().postForEntity(properties.getServiceUrl(), request, Map.class);
            return toResponse(response.getBody());
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    "Recognition service call failed: " + exception.getMessage());
        }
    }

    private Path resolveImagePath(String imageUrl) {
        return fileResourceService.resolveLocalPath(imageUrl);
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return new RestTemplate(requestFactory);
    }

    private HerbRecognitionClientResponse toResponse(Map<String, Object> body) {
        if (body == null) {
            throw new BusinessException("Recognition service returned empty response");
        }
        Map<String, Object> payload = unwrapPayload(body);
        List<HerbRecognitionCandidateVO> candidates = parseCandidates(payload.get("results"));
        HerbRecognitionCandidateVO topCandidate = candidates.isEmpty() ? null : candidates.get(0);
        HerbRecognitionClientResponse response = new HerbRecognitionClientResponse();
        response.setSuccess(resolveSuccess(body, payload, topCandidate));
        response.setPredictedName(resolvePredictedName(payload, topCandidate));
        response.setConfidence(resolveConfidence(payload, topCandidate));
        response.setCandidates(candidates);
        response.setRawResult(toJson(body));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapPayload(Map<String, Object> body) {
        Object data = body.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return (Map<String, Object>) dataMap;
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private List<HerbRecognitionCandidateVO> parseCandidates(Object value) {
        if (!(value instanceof List<?> records)) {
            return List.of();
        }
        List<HerbRecognitionCandidateVO> candidates = new ArrayList<>();
        for (Object record : records) {
            if (!(record instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) rawMap;
            HerbRecognitionCandidateVO candidate = new HerbRecognitionCandidateVO();
            candidate.setRank(valueAsInteger(item.get("rank")));
            candidate.setName(
                    firstText(
                            item.get("speciesName"), item.get("predictedName"), item.get("name")));
            candidate.setConfidence(valueAsBigDecimal(item.get("confidence")));
            candidate.setSimilarity(valueAsBigDecimal(item.get("similarity")));
            candidates.add(candidate);
        }
        return candidates;
    }

    private Boolean resolveSuccess(
            Map<String, Object> body,
            Map<String, Object> payload,
            HerbRecognitionCandidateVO topCandidate) {
        if (body.containsKey("success")) {
            return Boolean.TRUE.equals(body.get("success"));
        }
        if (payload.containsKey("success")) {
            return Boolean.TRUE.equals(payload.get("success"));
        }
        if (body.containsKey("code")) {
            return Integer.valueOf(200).equals(valueAsInteger(body.get("code")))
                    && topCandidate != null;
        }
        return topCandidate != null && topCandidate.getName() != null;
    }

    private String resolvePredictedName(
            Map<String, Object> body, HerbRecognitionCandidateVO topCandidate) {
        String predictedName =
                firstText(body.get("predictedName"), body.get("speciesName"), body.get("name"));
        if (predictedName != null) {
            return predictedName;
        }
        return topCandidate == null ? null : topCandidate.getName();
    }

    private BigDecimal resolveConfidence(
            Map<String, Object> body, HerbRecognitionCandidateVO topCandidate) {
        BigDecimal confidence = valueAsBigDecimal(body.get("confidence"));
        if (confidence != null) {
            return confidence;
        }
        return topCandidate == null ? null : topCandidate.getConfidence();
    }

    private HerbRecognitionClientResponse mockResponse() {
        HerbRecognitionCandidateVO candidate = new HerbRecognitionCandidateVO();
        candidate.setName("黄连");
        candidate.setConfidence(new BigDecimal("0.90"));
        HerbRecognitionClientResponse response = new HerbRecognitionClientResponse();
        response.setSuccess(true);
        response.setPredictedName("黄连");
        response.setConfidence(new BigDecimal("0.90"));
        response.setCandidates(List.of(candidate));
        response.setRawResult("{\"success\":true,\"predictedName\":\"黄连\",\"confidence\":0.90}");
        return response;
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            String text = valueAsString(value);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private Integer valueAsInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private BigDecimal valueAsBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Failed to serialize recognition result");
        }
    }
}
