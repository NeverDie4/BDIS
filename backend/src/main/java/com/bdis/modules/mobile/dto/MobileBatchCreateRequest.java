package com.bdis.modules.mobile.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MobileBatchCreateRequest {

    private String batchName;

    private Long baseId;

    private String baseName;

    private String originPlace;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime collectStartTime;

    private String remark;

    private Long collectorId;

    private String collectorName;
}
