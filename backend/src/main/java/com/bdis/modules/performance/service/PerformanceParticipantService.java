package com.bdis.modules.performance.service;

import com.bdis.modules.performance.dto.PerformanceParticipantRequest;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.vo.PerformanceParticipantUserVO;
import java.util.List;

public interface PerformanceParticipantService {

    List<PerformanceParticipantEntity> listParticipants(Long performanceId);

    List<PerformanceParticipantUserVO> listParticipantUsers(Long performanceId);

    PerformanceParticipantEntity addParticipant(
            Long performanceId, PerformanceParticipantRequest request);

    PerformanceParticipantEntity updateParticipant(
            Long performanceId, Long participantId, PerformanceParticipantRequest request);

    void removeParticipant(Long performanceId, Long participantId);
}
