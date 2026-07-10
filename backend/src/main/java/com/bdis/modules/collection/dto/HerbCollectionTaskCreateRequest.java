package com.bdis.modules.collection.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbCollectionTaskCreateRequest {

    @NotBlank private String taskCode;

    @NotBlank private String taskName;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String collectPlace;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime plannedStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime plannedEndTime;

    private Long collectorId;

    private String collectorName;

    private String taskStatus;

    private String description;

    private String remark;
}
