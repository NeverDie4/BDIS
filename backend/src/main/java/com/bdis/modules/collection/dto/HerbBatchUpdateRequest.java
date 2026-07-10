package com.bdis.modules.collection.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbBatchUpdateRequest {

    @NotBlank private String batchName;

    private Long taskId;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String originPlace;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime collectStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime collectEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime harvestTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    private String batchStatus;

    private String traceCode;

    private String remark;
}
