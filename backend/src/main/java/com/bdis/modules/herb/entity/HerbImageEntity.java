package com.bdis.modules.herb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_image")
public class HerbImageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String imageCode;

    private Long speciesId;

    private String imageUrl;

    private String imageName;

    private String uploadSource;

    private Long collectorId;

    private Long baseId;

    private String collectPlace;

    private LocalDateTime collectTime;

    private String imageType;

    private String growthStage;

    private String healthStatus;

    private String processStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
