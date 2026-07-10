package com.bdis.modules.collection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_collection_task")
public class HerbCollectionTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;

    private String taskName;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String collectPlace;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private Long collectorId;

    private String collectorName;

    private String taskStatus;

    private String description;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
