package com.bdis.modules.collection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_batch_image")
public class HerbBatchImageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private Long imageId;

    private Long identificationResultId;

    private String imageRole;

    private Integer isPrimary;

    private String bindStatus;

    private Integer sortOrder;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
