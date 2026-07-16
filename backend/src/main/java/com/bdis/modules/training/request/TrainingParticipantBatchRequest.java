package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class TrainingParticipantBatchRequest {
    @NotEmpty private List<@NotNull @Positive Long> userIds;

    @Size(max = 500)
    private String remark;
}
