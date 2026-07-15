package com.bdis.modules.performance.vo;

import com.bdis.modules.performance.entity.PerformanceAuditEntity;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.entity.PerformanceStandardEntity;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceDetailVO {

    private PerformanceEntity performance;

    private PerformanceStandardEntity standard;

    private List<PerformanceMaterialVO> materials;

    private List<PerformanceParticipantEntity> participants;

    private List<PerformanceAuditEntity> auditRecords;
}
