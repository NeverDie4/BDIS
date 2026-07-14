package com.bdis.modules.experiment.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ExperimentRecordSubmitRequest {

    @NotNull @PositiveOrZero private Integer version;
}
