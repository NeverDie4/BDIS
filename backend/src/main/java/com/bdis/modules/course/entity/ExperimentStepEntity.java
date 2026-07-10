package com.bdis.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_experiment_step")
public class ExperimentStepEntity extends BaseEntity {

    private Long courseId;

    private String stepNo;

    private String stepTitle;

    private String stepContent;

    private String expectedResult;

    private Integer sortOrder;
}
