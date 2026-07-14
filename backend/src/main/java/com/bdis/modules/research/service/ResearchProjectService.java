package com.bdis.modules.research.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.research.query.ResearchProjectQuery;
import com.bdis.modules.research.request.ResearchProjectCreateRequest;
import com.bdis.modules.research.request.ResearchProjectLeaderChangeRequest;
import com.bdis.modules.research.request.ResearchProjectStatusChangeRequest;
import com.bdis.modules.research.request.ResearchProjectUpdateRequest;
import com.bdis.modules.research.vo.ResearchProjectDetailVO;
import com.bdis.modules.research.vo.ResearchProjectListVO;

public interface ResearchProjectService {
    PageResult<ResearchProjectListVO> page(ResearchProjectQuery query);

    ResearchProjectDetailVO getDetail(Long id);

    Long create(ResearchProjectCreateRequest request);

    void update(Long id, ResearchProjectUpdateRequest request);

    void changeLeader(Long id, ResearchProjectLeaderChangeRequest request);

    void changeStatus(Long id, ResearchProjectStatusChangeRequest request);
}
