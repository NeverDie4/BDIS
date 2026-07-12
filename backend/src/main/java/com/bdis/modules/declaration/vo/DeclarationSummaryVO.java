package com.bdis.modules.declaration.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationSummaryVO {

    private Long declarationId;

    private String applicationTitle;

    private String reviewStatus;

    private Long materialCount;

    private Long reviewRecordCount;

    private Long archiveId;

    private String archiveNo;

    private Long archiveItemCount;
}
