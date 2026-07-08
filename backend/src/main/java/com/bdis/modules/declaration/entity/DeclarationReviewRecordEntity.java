package com.bdis.modules.declaration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_review_record")
public class DeclarationReviewRecordEntity extends CreateAuditEntity {

    private Long applicationId;

    private Long reviewerId;

    private String reviewAction;

    private String beforeStatus;

    private String reviewStatus;

    private String reviewComment;

    private LocalDateTime reviewedAt;
}
