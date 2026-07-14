package com.bdis.modules.research.query;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResearchProjectQuery {
    private String keyword;
    private String projectType;
    private Long leaderId;
    private Long speciesId;
    private String projectStatus;
    private LocalDateTime startedFrom;
    private LocalDateTime startedTo;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
