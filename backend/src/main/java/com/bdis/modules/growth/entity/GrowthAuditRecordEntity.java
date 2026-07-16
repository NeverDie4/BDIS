package com.bdis.modules.growth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_growth_review_record")
public class GrowthAuditRecordEntity extends CreateAuditEntity {

    private Long growthRecordId;

    private Long reviewerId;

    private String reviewerName;

    private String reviewerRole;

    private String reviewAction;

    private String beforeStatus;

    private String afterStatus;

    private String reviewComment;

    private LocalDateTime reviewedAt;
}
