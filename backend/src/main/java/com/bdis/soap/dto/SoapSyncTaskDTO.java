package com.bdis.soap.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SoapSyncTaskDTO {

    @NotBlank private String resourceType;

    private String serviceName;
    private String methodName;
    private String direction = "INBOUND";
    private Boolean mock = true;
    private String requestXml;
    private String remark;
}
