package com.bdis.modules.experiment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExperimentRecordUpdateRequest {

    @NotBlank
    @Size(max = 200)
    private String experimentTitle;

    private String experimentProcess;

    private String experimentResult;

    private LocalDateTime recordedAt;

    @Size(max = 500)
    private String remark;

    @NotNull private Integer version;

    @Null(message = "recordNo cannot be changed")
    private String recordNo;

    @Null(message = "courseId cannot be changed")
    private Long courseId;

    @Null(message = "projectId cannot be changed")
    private Long projectId;

    @Null(message = "recorderId cannot be changed")
    private Long recorderId;

    @Null(message = "archiveStatus must use the dedicated workflow")
    private String archiveStatus;

    @Null(message = "submittedAt must use the dedicated workflow")
    private LocalDateTime submittedAt;

    @Null(message = "submittedBy must use the dedicated workflow")
    private Long submittedBy;

    @Null(message = "archivedAt must use the dedicated workflow")
    private LocalDateTime archivedAt;

    @Null(message = "archivedBy must use the dedicated workflow")
    private Long archivedBy;

    @Null(message = "archiveComment must use the dedicated workflow")
    private String archiveComment;
}
