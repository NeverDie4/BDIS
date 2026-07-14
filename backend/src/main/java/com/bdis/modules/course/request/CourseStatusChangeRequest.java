package com.bdis.modules.course.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseStatusChangeRequest {

    @NotNull(message = "version is required")
    private Integer version;
}
