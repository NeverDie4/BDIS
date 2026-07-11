package com.bdis.modules.soap.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("soap_exchange_record")
public class SoapExchangeRecordEntity extends BasicEntity {

    private Long taskId;

    private String exchangeNo;

    private String serviceName;

    private String methodName;

    private String requestXml;

    private String responseXml;

    private String syncStatus;

    private String errorMessage;

    private String parsedPayload;

    private Long calledBy;

    private String calledByName;

    private LocalDateTime calledAt;
}
