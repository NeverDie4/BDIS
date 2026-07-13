package com.bdis.modules.course.vo;

import java.time.LocalDateTime;
import java.util.List;
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
    private String publishStatus;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<ExperimentStepVO> steps = List.of();
    private List<CourseResourceVO> resources = List.of();
}
