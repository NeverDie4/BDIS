package com.bdis.modules.settings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_user_preference")
public class UserPreferenceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String preferenceNamespace;

    private String preferenceData;

    private Integer schemaVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version private Integer version;
}
