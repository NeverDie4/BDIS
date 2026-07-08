package com.bdis.modules.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_file_business")
public class FileBusinessEntity extends CreateAuditEntity {

    private Long fileId;

    private String bizType;

    private Long bizId;

    private String fileUsage;

    private Integer sortOrder;
}
