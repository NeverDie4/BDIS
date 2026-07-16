package com.bdis.modules.course.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class CourseRelationUpdateRequest {

    @NotNull(message = "version is required")
    private Integer version;

    private List<Long> speciesIds = List.of();

    private List<Long> projectIds = List.of();
}
