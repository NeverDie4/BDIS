package com.bdis.soap.integration;

import lombok.Data;

@Data
public class SoapBusinessImportResult {

    private String status;
    private String businessType;
    private Long businessId;
    private String externalNo;
    private Integer successCount = 0;
    private Integer failureCount = 0;
    private String message;

    public static SoapBusinessImportResult prepared(String externalNo, String message) {
        SoapBusinessImportResult result = new SoapBusinessImportResult();
        result.setStatus("PREPARED");
        result.setBusinessType("herb_growth_record");
        result.setExternalNo(externalNo);
        result.setMessage(message);
        return result;
    }

    public static SoapBusinessImportResult imported(
            Long businessId, String externalNo, String message) {
        SoapBusinessImportResult result = new SoapBusinessImportResult();
        result.setStatus("SUCCESS");
        result.setBusinessType("herb_growth_record");
        result.setBusinessId(businessId);
        result.setExternalNo(externalNo);
        result.setSuccessCount(1);
        result.setMessage(message);
        return result;
    }
}
