package com.bdis.modules.spectrum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class HerbIdentificationReviewRequest {

    private Long finalSpeciesId;

    @NotBlank(message = "复核状态不能为空")
    @Pattern(regexp = "confirmed|rejected", message = "复核状态只能是 confirmed 或 rejected")
    private String reviewStatus;

    private String reviewComment;
}
