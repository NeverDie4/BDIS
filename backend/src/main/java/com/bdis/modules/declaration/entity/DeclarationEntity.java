package com.bdis.modules.declaration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_application")
public class DeclarationEntity extends BaseEntity {

    private String applicationNo;

    private String applicationTitle;

    private String applicationType;

    private Long applicantId;

    private String reviewStatus;

    private LocalDateTime submittedAt;

    private Long reviewerId;

    private LocalDateTime reviewedAt;

    private String reviewComment;
}
