package com.bdis.modules.research.service;

import com.bdis.modules.research.entity.ResearchProjectTaskEntity;
import com.bdis.modules.research.request.ResearchProjectTaskCreateRequest;
import java.util.List;
import com.bdis.modules.research.entity.ResearchProjectTaskMemberEntity;
import com.bdis.modules.research.request.ResearchProjectTaskMemberRequest;

public interface ResearchProjectTaskService {
    Long create(Long projectId, ResearchProjectTaskCreateRequest request);
    List<ResearchProjectTaskEntity> list(Long projectId);
    void accept(Long taskId);
    void assignMember(Long taskId, ResearchProjectTaskMemberRequest request);
    List<ResearchProjectTaskMemberEntity> members(Long taskId);
}
