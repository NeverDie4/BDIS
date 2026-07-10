package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_ai_model_version")
public class AiModelVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String modelCode;

    private String modelName;

    private String version;

    private String modelType;

    private String modelPath;

    private BigDecimal accuracy;

    private Integer status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
