package com.bdis.soap.integration;

public interface SoapBusinessImportHandler {

    boolean supports(String resourceType);

    SoapBusinessImportResult importData(SoapBusinessImportContext context);
}
