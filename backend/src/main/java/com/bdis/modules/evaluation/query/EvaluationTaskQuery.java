package com.bdis.modules.evaluation.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationTaskQuery {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private String targetType;

    private Long targetId;

    private String status;
}
