package com.bdis.modules.course.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CourseUpdateRequest {

    @NotBlank(message = "courseNo is required")
    @Size(max = 64, message = "courseNo must not exceed 64 characters")
    private String courseNo;

    @NotBlank(message = "courseName is required")
    @Size(max = 150, message = "courseName must not exceed 150 characters")
    private String courseName;

    @NotBlank(message = "courseType is required")
    @Size(max = 50, message = "courseType must not exceed 50 characters")
    private String courseType;

    @NotNull(message = "teacherId is required")
    private Long teacherId;

    @NotNull(message = "version is required")
    private Integer version;

    private String description;

    @Size(max = 500, message = "videoUrl must not exceed 500 characters")
    private String videoUrl;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
