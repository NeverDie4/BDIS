package com.bdis.modules.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationTaskRequest {

    private String taskNo;

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    private String taskType;

    @NotBlank(message = "评价对象类型不能为空")
    private String targetType;

    @NotNull(message = "评价对象 ID 不能为空")
    private Long targetId;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer status;

    private String remark;
}
