package com.bdis.modules.course.query;

import com.bdis.common.core.BaseQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseQuery extends BaseQuery {
    private String courseType;
    private String publishStatus;
    private Long teacherId;
}
