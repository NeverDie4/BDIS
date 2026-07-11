package com.bdis.modules.evaluation.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationStandardQuery {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private String indicatorType;

    private Integer status;
}
