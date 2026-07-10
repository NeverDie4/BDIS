package com.bdis.modules.herb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_species")
public class HerbEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String herbCode;

    private String herbName;

    private String latinName;

    private String aliasName;

    private String category;

    private String medicinalPart;

    private String efficacy;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
