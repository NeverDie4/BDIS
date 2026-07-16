package com.bdis.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationDTO {

    @NotBlank(message = "机构编号不能为空")
    private String organizationNo;

    @NotBlank(message = "机构名称不能为空")
    private String organizationName;

    private String organizationType;

    private String contactName;

    private String contactPhone;

    private String address;

    private Integer status;
}
