package com.bdis.modules.course.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExperimentStepVO {

    private Long id;
    private Long courseId;
    private String stepNo;
    private String stepTitle;
    private String stepContent;
    private String expectedResult;
    private Integer sortOrder;
    private Integer status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String remark;
}
