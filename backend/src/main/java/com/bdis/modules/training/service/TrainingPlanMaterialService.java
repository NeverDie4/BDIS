package com.bdis.modules.training.service;

import com.bdis.modules.training.request.TrainingPlanMaterialBindRequest;
import com.bdis.modules.training.vo.TrainingPlanMaterialVO;
import java.util.List;

public interface TrainingPlanMaterialService {
    List<TrainingPlanMaterialVO> list(Long planId);

    Long bind(Long planId, TrainingPlanMaterialBindRequest request);

    void unbind(Long planId, Long materialId);

    boolean hasValidMaterial(Long planId);
}
