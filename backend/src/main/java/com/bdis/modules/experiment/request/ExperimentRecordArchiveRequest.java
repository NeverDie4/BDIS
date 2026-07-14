package com.bdis.modules.experiment.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExperimentRecordArchiveRequest {

    @NotNull @PositiveOrZero private Integer version;

    @Size(max = 500)
    private String archiveComment;
}
