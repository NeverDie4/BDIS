package com.bdis.modules.performance.vo;

import com.bdis.file.vo.FileBusinessVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceMaterialVO {

    private Long id;

    private Long fileId;

    private String bizType;

    private Long bizId;

    private String fileUsage;

    private Integer sortOrder;

    private Long createdBy;

    private String remark;

    public static PerformanceMaterialVO fromFileBusiness(FileBusinessVO entity) {
        PerformanceMaterialVO vo = new PerformanceMaterialVO();
        vo.setId(entity.getId());
        vo.setFileId(entity.getFileId());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setFileUsage(entity.getFileUsage());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
