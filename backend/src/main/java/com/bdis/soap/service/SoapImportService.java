package com.bdis.soap.service;

import com.bdis.soap.vo.SoapImportResultVO;

public interface SoapImportService {

    SoapImportResultVO parseAndPrepareImport(String resourceType, String responseXml);
}
