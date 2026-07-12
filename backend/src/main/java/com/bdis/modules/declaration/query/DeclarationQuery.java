package com.bdis.modules.declaration.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationQuery {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private String applicationType;

    private String status;

    private Long applicantId;
}
