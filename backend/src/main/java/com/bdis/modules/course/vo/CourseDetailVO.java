package com.bdis.modules.course.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CourseDetailVO {

    private Long id;
    private String courseNo;
    private String courseName;
    private String courseType;
    private Long teacherId;
    private String teacherName;
    private String description;
    private String videoUrl;
    private List<String> applicableMajors = List.of();
    private Integer hours;
    private BigDecimal credits;
    private List<String> prerequisites = List.of();
    private List<String> teachingObjectives = List.of();
    private List<String> teachingMethods = List.of();
    private List<String> tags = List.of();
    private String publishStatus;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private String publisherName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Integer version;
    private List<ExperimentStepVO> steps = List.of();
    private List<CourseResourceVO> resources = List.of();
}
