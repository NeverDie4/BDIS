package com.bdis.modules.performance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.modules.performance.dto.PerformanceParticipantRequest;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.vo.PerformanceParticipantUserVO;
import com.bdis.modules.performance.vo.PerformanceParticipantVO;
import java.util.List;

public interface PerformanceParticipantService {

    List<PerformanceParticipantVO> listParticipants(Long performanceId);

    IPage<PerformanceParticipantUserVO> listParticipantUsers(
            Long performanceId, String keyword, Long pageNum, Long pageSize);

    PerformanceParticipantEntity addParticipant(
            Long performanceId, PerformanceParticipantRequest request);

    PerformanceParticipantEntity updateParticipant(
            Long performanceId, Long participantId, PerformanceParticipantRequest request);

    void removeParticipant(Long performanceId, Long participantId);
}
