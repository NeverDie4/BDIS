package com.bdis.modules.training.service;

import com.bdis.modules.training.entity.TrainingPlanItemEntity;
import com.bdis.modules.training.request.TrainingPlanItemRequest;
import java.util.List;

public interface TrainingPlanItemService { List<TrainingPlanItemEntity> list(Long planId); Long create(Long planId, TrainingPlanItemRequest request); void update(Long id, TrainingPlanItemRequest request); void delete(Long id); }
