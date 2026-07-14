package com.bdis.modules.research.vo;

import lombok.Data;

@Data
public class ResearchUserCandidateVO {
    private Long id;
    private String username;
    private String realName;
    private String userType;
    private Integer status;
}
