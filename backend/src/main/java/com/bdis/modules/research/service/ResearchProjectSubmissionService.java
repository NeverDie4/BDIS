package com.bdis.modules.research.service;

import com.bdis.modules.research.request.ResearchProjectSubmissionCreateRequest;
import com.bdis.modules.research.request.ResearchProjectSubmissionReviewRequest;

public interface ResearchProjectSubmissionService {
    Long submit(Long projectId, ResearchProjectSubmissionCreateRequest request);
    void review(Long submissionId, ResearchProjectSubmissionReviewRequest request);
}
