package com.bdis.modules.research.service;

import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import com.bdis.modules.research.request.ResearchProjectSubmissionCreateRequest;
import com.bdis.modules.research.request.ResearchProjectSubmissionReviewRequest;
import java.util.List;

public interface ResearchProjectSubmissionService {
    Long submit(Long projectId, ResearchProjectSubmissionCreateRequest request);

    List<ResearchProjectSubmissionEntity> list(Long projectId);

    void review(Long submissionId, ResearchProjectSubmissionReviewRequest request);
}
