package com.bdis.modules.growth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GrowthAuditCommentRequest {

    @Size(max = 500)
    private String comment;
}
