package com.bdis.soap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("soap_exchange_record")
public class SoapExchangeRecordEntity {

    @TableId(type = IdType.AUTO)
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
    private Long calledBy;
    private String calledByName;
    private LocalDateTime calledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String remark;
}
