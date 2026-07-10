package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_image_recognition")
public class ImageRecognitionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long imageId;

    private Long modelVersionId;

    private Long predictedSpeciesId;

    private String predictedName;

    private BigDecimal confidence;

    private String recognitionStatus;

    private LocalDateTime recognitionTime;

    private String rawResult;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
