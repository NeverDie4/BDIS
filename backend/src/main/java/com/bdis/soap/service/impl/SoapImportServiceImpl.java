package com.bdis.soap.service.impl;

import com.bdis.soap.component.SoapXmlParser;
import com.bdis.soap.integration.SoapBusinessImportContext;
import com.bdis.soap.integration.SoapBusinessImportHandler;
import com.bdis.soap.integration.SoapBusinessImportResult;
import com.bdis.soap.service.SoapImportService;
import com.bdis.soap.vo.SoapImportResultVO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SoapImportServiceImpl implements SoapImportService {

    private final SoapXmlParser soapXmlParser;
    private final List<SoapBusinessImportHandler> importHandlers;

    public SoapImportServiceImpl(
            SoapXmlParser soapXmlParser, List<SoapBusinessImportHandler> importHandlers) {
        this.soapXmlParser = soapXmlParser;
        this.importHandlers = importHandlers;
    }

    @Override
    public SoapImportResultVO parseAndPrepareImport(String resourceType, String responseXml) {
        Map<String, Object> parsedData = soapXmlParser.parseGrowthRecord(responseXml);
        SoapBusinessImportResult importResult =
                importHandlers.stream()
                        .filter(handler -> handler.supports(resourceType))
                        .findFirst()
                        .map(handler -> handler.importData(toContext(resourceType, responseXml, parsedData)))
                        .orElseGet(() -> SoapBusinessImportResult.prepared(
                                text(parsedData, "externalNo"),
                                "SOAP 数据已解析；待 M09 生长采集 Service 接入后可直接导入。"));
        SoapImportResultVO result = new SoapImportResultVO();
        result.setResourceType(resourceType);
        result.setStatus(importResult.getStatus());
        result.setBusinessType(importResult.getBusinessType());
        result.setBusinessId(importResult.getBusinessId());
        result.setExternalNo(importResult.getExternalNo());
        result.setSuccessCount(importResult.getSuccessCount());
        result.setFailureCount(importResult.getFailureCount());
        result.setParsedData(parsedData);
        result.setMessage(importResult.getMessage());
        return result;
    }

    private SoapBusinessImportContext toContext(
            String resourceType, String responseXml, Map<String, Object> parsedData) {
        SoapBusinessImportContext context = new SoapBusinessImportContext();
        context.setResourceType(resourceType);
        context.setRawXml(responseXml);
        context.setParsedData(parsedData);
        return context;
    }

    private String text(Map<String, Object> parsedData, String key) {
        Object value = parsedData.get(key);
        return value == null ? null : value.toString();
    }
}
