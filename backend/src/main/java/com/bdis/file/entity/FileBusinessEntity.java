package com.bdis.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_file_business")
public class FileBusinessEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;
    private String bizType;
    private Long bizId;
    private String fileUsage;
    private LocalDateTime createdAt;
    private Long createdBy;
    private String remark;
}
