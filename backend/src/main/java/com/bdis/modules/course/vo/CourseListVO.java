package com.bdis.modules.course.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CourseListVO {

    private Long id;
    private String courseNo;
    private String courseName;
    private String courseType;
    private Long teacherId;
    private String teacherName;
    private String publishStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
