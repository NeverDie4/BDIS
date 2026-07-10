package com.bdis.modules.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("auth_menu")
public class MenuEntity extends BaseEntity {

    private Long parentId;

    private String menuCode;

    private String menuName;

    private String routePath;

    private String componentPath;

    private String icon;

    private Integer visible;

    private Integer sortOrder;
}
