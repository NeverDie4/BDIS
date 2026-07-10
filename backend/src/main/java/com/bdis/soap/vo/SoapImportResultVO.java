package com.bdis.soap.vo;

import java.util.Map;
import lombok.Data;

@Data
public class SoapImportResultVO {

    private String resourceType;
    private String status;
    private String businessType;
    private Long businessId;
    private String externalNo;
    private Integer successCount;
    private Integer failureCount;
    private Map<String, Object> parsedData;
    private String message;
}
