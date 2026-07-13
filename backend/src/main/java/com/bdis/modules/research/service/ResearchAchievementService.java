package com.bdis.modules.research.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.research.query.ResearchAchievementQuery;
import com.bdis.modules.research.request.ResearchAchievementCreateRequest;
import com.bdis.modules.research.request.ResearchAchievementUpdateRequest;
import com.bdis.modules.research.vo.ResearchAchievementDetailVO;
import com.bdis.modules.research.vo.ResearchAchievementListVO;
import com.bdis.modules.research.vo.ResearchAchievementSummaryVO;
import java.util.List;

public interface ResearchAchievementService {
    PageResult<ResearchAchievementListVO> page(ResearchAchievementQuery query);
    ResearchAchievementDetailVO getDetail(Long id);
    Long create(ResearchAchievementCreateRequest request);
    void update(Long id, ResearchAchievementUpdateRequest request);
    List<ResearchAchievementListVO> listByProjectId(Long projectId);
    ResearchAchievementSummaryVO summarizeByProjectId(Long projectId);
}
