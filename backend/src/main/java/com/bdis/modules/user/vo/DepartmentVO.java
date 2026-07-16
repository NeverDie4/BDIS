package com.bdis.modules.user.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentVO {

    private Long id;

    private String departmentNo;

    private String departmentName;

    private Long organizationId;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;

    private List<DepartmentVO> children = new ArrayList<>();
}
