package com.bdis.modules.spectrum.client;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.spectrum.config.HerbFeatureProperties;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class FeatureExtractionClientImpl implements FeatureExtractionClient {

    private final HerbFeatureProperties properties;

    public FeatureExtractionClientImpl(HerbFeatureProperties properties) {
        this.properties = properties;
    }

    @Override
    public FeatureExtractionClientResponse extract(Path imagePath) {
        if (!properties.isEnabled() || properties.isMockEnabled()) {
            return mockResponse(imagePath);
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imagePath));
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        org.springframework.http.HttpEntity<MultiValueMap<String, Object>> request =
                new org.springframework.http.HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response =
                    restTemplate().postForEntity(properties.getServiceUrl(), request, Map.class);
            return parseResponse(response.getBody());
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    "Feature extraction service call failed: " + exception.getMessage());
        }
    }

    private FeatureExtractionClientResponse mockResponse(Path imagePath) {
        int dimension = properties.getMockDimension() == null ? 512 : properties.getMockDimension();
        Random random = new Random(imagePath.toString().hashCode());
        List<Double> vector = new ArrayList<>(dimension);
        for (int index = 0; index < dimension; index++) {
            vector.add(random.nextDouble() * 2 - 1);
        }
        FeatureExtractionClientResponse response = new FeatureExtractionClientResponse();
        response.setSuccess(true);
        response.setFeatureVector(vector);
        response.setDimension(dimension);
        response.setModelName(properties.getModelName());
        response.setModelVersion(properties.getModelVersion());
        response.setMessage("Feature extracted successfully");
        return response;
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return new RestTemplate(requestFactory);
    }

    private FeatureExtractionClientResponse parseResponse(Map<String, Object> body) {
        if (body == null) {
            throw new BusinessException("Feature extraction service returned empty response");
        }
        if (body.containsKey("data") && body.get("data") instanceof Map<?, ?> data) {
            return parseFeatureAppResponse(body, data);
        }
        FeatureExtractionClientResponse response = new FeatureExtractionClientResponse();
        response.setSuccess(Boolean.TRUE.equals(body.get("success")));
        response.setFeatureVector(toDoubleList(body.get("featureVector")));
        response.setDimension(toInteger(body.get("dimension")));
        response.setModelName(valueAsString(body.get("modelName")));
        response.setModelVersion(valueAsString(body.get("modelVersion")));
        response.setMessage(valueAsString(body.get("message")));
        return response;
    }

    private FeatureExtractionClientResponse parseFeatureAppResponse(
            Map<String, Object> body, Map<?, ?> data) {
        FeatureExtractionClientResponse response = new FeatureExtractionClientResponse();
        response.setSuccess(Integer.valueOf(200).equals(toInteger(body.get("code"))));
        response.setFeatureVector(toDoubleList(data.get("vector")));
        response.setDimension(toInteger(data.get("dim")));
        response.setModelName(properties.getModelName());
        response.setModelVersion(valueAsString(data.get("modelVersion")));
        response.setMessage(valueAsString(body.get("msg")));
        return response;
    }

    private List<Double> toDoubleList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(item -> ((Number) item).doubleValue()).toList();
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }
}
