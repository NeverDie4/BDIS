package com.bdis.soap.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SoapExchangeRecordVO {

    private Long id;
    private String exchangeNo;
    private Long taskId;
    private String serviceName;
    private String methodName;
    private String requestXml;
    private String responseXml;
    private String exchangeStatus;
    private String errorMessage;
    private String parsedPayload;
    private LocalDateTime calledAt;
}
