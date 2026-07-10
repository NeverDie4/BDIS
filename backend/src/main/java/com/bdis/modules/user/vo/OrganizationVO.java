package com.bdis.modules.user.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationVO {

    private Long id;

    private String organizationNo;

    private String organizationName;

    private String organizationType;

    private String contactName;

    private String contactPhone;

    private String address;

    private Integer status;
}
