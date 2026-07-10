package com.bdis.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_department")
public class DepartmentEntity extends BaseEntity {

    private String departmentNo;

    private String departmentName;

    private Long organizationId;

    private Long parentId;

    private Integer sortOrder;
}
