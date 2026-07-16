package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ImageObservation;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.SnapshotImageObservation;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentFieldDataMapper {

  List<HerbBatchEntity> selectValidBatches(@Param("taskId") Long taskId);

  GrowthRecordEntity selectLatestGrowthRecord(@Param("taskId") Long taskId);

  List<GrowthRecordEntity> selectGrowthRecords(@Param("taskId") Long taskId);

  List<ImageObservation> selectImages(@Param("taskId") Long taskId);

  List<SnapshotImageObservation> selectSnapshotImages(@Param("taskId") Long taskId);
}
