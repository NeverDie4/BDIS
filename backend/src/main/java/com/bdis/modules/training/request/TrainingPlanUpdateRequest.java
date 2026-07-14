package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingPlanUpdateRequest {
    @NotBlank
    @Size(max = 200)
    private String planName;

    @NotBlank
    @Size(max = 50)
    private String planType;

    @NotNull @Positive private Long ownerId;
    @Positive private Long courseId;
    @Positive private Long trainerId;
    private String description;

    @Size(max = 255)
    private String location;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Size(max = 500)
    private String remark;

    @NotNull
    @jakarta.validation.constraints.Min(0)
    private Integer version;

    @Null(message = "planNo cannot be changed")
    private String planNo;

    @Null(message = "publishStatus must use the publish workflow")
    private String publishStatus;

    @Null(message = "publishedAt must use the publish workflow")
    private LocalDateTime publishedAt;

    @Null(message = "publishedBy must use the publish workflow")
    private Long publishedBy;
}
