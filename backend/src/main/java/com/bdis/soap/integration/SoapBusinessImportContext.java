package com.bdis.soap.integration;

import java.util.Map;
import lombok.Data;

@Data
public class SoapBusinessImportContext {

    private String resourceType;
    private Map<String, Object> parsedData;
    private String rawXml;
}
