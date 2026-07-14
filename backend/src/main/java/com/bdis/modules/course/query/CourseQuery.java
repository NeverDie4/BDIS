package com.bdis.modules.course.query;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CourseQuery {

    private String keyword;

    private String courseType;

    private Long teacherId;

    private String publishStatus;

    private Integer status;

    private LocalDateTime startedFrom;

    private LocalDateTime startedTo;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
