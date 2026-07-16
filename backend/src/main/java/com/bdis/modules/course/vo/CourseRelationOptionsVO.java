package com.bdis.modules.course.vo;

import java.util.List;
import lombok.Data;

@Data
public class CourseRelationOptionsVO {

    private List<CourseRelationOptionVO> herbs = List.of();
    private List<CourseRelationOptionVO> projects = List.of();
}
