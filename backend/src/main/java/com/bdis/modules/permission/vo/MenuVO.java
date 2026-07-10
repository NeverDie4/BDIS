package com.bdis.modules.permission.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuVO {

    private Long id;

    private Long parentId;

    private String menuCode;

    private String menuName;

    private String routePath;

    private String componentPath;

    private String icon;

    private Integer visible;

    private Integer sortOrder;

    private Integer status;

    private List<MenuVO> children = new ArrayList<>();
}
