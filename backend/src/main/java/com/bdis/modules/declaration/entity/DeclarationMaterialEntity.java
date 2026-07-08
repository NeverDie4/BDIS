package com.bdis.modules.declaration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_attachment")
public class DeclarationMaterialEntity extends BaseEntity {

    private Long applicationId;

    private Long fileId;

    private String fileName;

    private String fileType;

    private String fileUrl;

    private Long fileSize;

    private Long uploaderId;

    private LocalDateTime uploadedAt;
}
