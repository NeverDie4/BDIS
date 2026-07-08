package com.bdis.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_organization")
public class OrganizationEntity extends BaseEntity {

    private String organizationNo;

    private String organizationName;

    private String organizationType;

    private String contactName;

    private String contactPhone;

    private String address;
}
