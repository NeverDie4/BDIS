package com.bdis.modules.training.service;

import com.bdis.modules.training.vo.TrainingSummaryVO;

public interface TrainingSummaryService {
    TrainingSummaryVO getSummary(Long planId);
}
