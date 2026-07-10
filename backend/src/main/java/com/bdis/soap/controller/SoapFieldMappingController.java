package com.bdis.soap.controller;

import com.bdis.common.response.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/soap-field-mappings")
public class SoapFieldMappingController {

    @GetMapping
    public ApiResponse<List<Map<String, String>>> mappings() {
        return ApiResponse.success(
                List.of(
                        Map.of("xmlField", "externalNo", "businessField", "externalNo"),
                        Map.of("xmlField", "herbName", "businessField", "herbName"),
                        Map.of("xmlField", "baseName", "businessField", "baseName"),
                        Map.of("xmlField", "collectorName", "businessField", "collectorName"),
                        Map.of("xmlField", "collectedAt", "businessField", "collectedAt")));
    }
}
