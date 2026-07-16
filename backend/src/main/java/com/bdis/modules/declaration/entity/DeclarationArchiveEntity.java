package com.bdis.modules.declaration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_archive")
public class DeclarationArchiveEntity extends BaseEntity {

    private String archiveNo;

    private Long applicationId;

    private String archiveTitle;

    private Long ownerId;

    private String archiveStatus;

    private LocalDateTime generatedAt;
}
