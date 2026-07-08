package com.bdis.modules.declaration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_archive_item")
public class DeclarationArchiveItemEntity extends CreateAuditEntity {

    private Long archiveId;

    private String sourceType;

    private Long sourceId;

    private String itemName;

    private String itemDesc;

    private Integer sortOrder;
}
