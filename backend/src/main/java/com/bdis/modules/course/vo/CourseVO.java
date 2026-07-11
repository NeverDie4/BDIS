package com.bdis.modules.course.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CourseVO {
    private Long id;
    private String courseNo;
    private String courseName;
    private String courseType;
    private Long teacherId;
    private String teacherName;
    private String description;
    private String videoUrl;
    private String publishStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ExperimentStepVO> steps = new ArrayList<>();
    private List<CourseResourceVO> resources = new ArrayList<>();
}
