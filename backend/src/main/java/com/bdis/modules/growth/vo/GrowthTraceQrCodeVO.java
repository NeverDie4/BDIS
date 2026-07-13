package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GrowthTraceQrCodeVO {

    private Long recordId;

    private String traceCode;

    private String traceUrl;

    private String qrCodeUrl;

    private Integer publicVisible;

    private LocalDateTime traceGeneratedTime;
}
