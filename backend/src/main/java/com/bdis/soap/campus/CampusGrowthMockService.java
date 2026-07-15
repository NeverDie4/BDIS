package com.bdis.soap.campus;

import jakarta.jws.WebService;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;
import java.util.List;
import javax.xml.namespace.QName;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@WebService(
        endpointInterface = "com.bdis.soap.campus.CampusGrowthDataPort",
        targetNamespace = CampusGrowthDataPort.NAMESPACE,
        serviceName = "CampusGrowthDataService",
        portName = "CampusGrowthDataPort")
public class CampusGrowthMockService implements CampusGrowthDataPort {

    @Override
    public CampusGrowthQueryResponse queryGrowthRecords(CampusGrowthQueryRequest request) {
        String requestId = request == null ? null : request.getRequestId();
        if (!StringUtils.hasText(requestId)) {
            throw soapFault("requestId 不能为空");
        }
        if (requestId.startsWith("SOAP-FAULT-")) {
            throw soapFault("本地模拟校内 SOAP 服务故障");
        }
        if (requestId.startsWith("BUSINESS-FAILURE-")) {
            return response("FAILED", "本地模拟校内系统拒绝该请求", List.of());
        }

        CampusGrowthRecord record = new CampusGrowthRecord();
        record.setExternalNo("CAMPUS-DEMO-GROWTH-001");
        record.setHerbName("SOAP演示黄连");
        record.setBaseName("SOAP演示重庆基地");
        record.setCollectorName("校内系统模拟账号");
        record.setCollectedAt("2026-07-15T10:30:00");
        record.setSourceType("SOAP");
        return response("SUCCESS", "ok", List.of(record));
    }

    private CampusGrowthQueryResponse response(
            String code, String message, List<CampusGrowthRecord> records) {
        CampusGrowthQueryResponse response = new CampusGrowthQueryResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setRecords(records);
        return response;
    }

    private SOAPFaultException soapFault(String message) {
        try {
            SOAPFault fault =
                    SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL)
                            .createFault(
                                    message,
                                    new QName(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, "Server"));
            return new SOAPFaultException(fault);
        } catch (SOAPException exception) {
            throw new IllegalStateException("无法创建 SOAP Fault", exception);
        }
    }
}
