package com.bdis.modules.collection.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class HerbBatchImageBatchBindRequest {

    @Valid @NotEmpty private List<HerbBatchImageBindRequest> images;
}
