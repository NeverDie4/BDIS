package com.bdis.modules.collection.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class HerbBatchQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String batchStatus;

    private Long taskId;

    private Long speciesId;

    private Long baseId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private List<String> excludedStatuses;
}
