package com.bdis.modules.course.query;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseResourceQuery {

    @Size(max = 50, message = "resourceType must not exceed 50 characters")
    private String resourceType;
}
