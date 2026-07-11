package com.bdis.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CourseRequest {
    @NotBlank(message = "课程编号不能为空")
    @Size(max = 64)
    private String courseNo;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 150)
    private String courseName;

    private String courseType;
    private Long teacherId;
    private String description;
    private String videoUrl;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer status;
    private String remark;
}
