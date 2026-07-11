package com.bdis.modules.collection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_batch_image")
public class HerbBatchImageEntity extends BaseEntity {

    private Long batchId;

    private Long imageId;

    private Long identificationResultId;

    private String imageRole;

    private Integer isPrimary;

    private String bindStatus;

    private Integer sortOrder;
}
