package com.bdis.modules.collection.service;

import com.bdis.modules.collection.dto.HerbBatchImageBatchBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageQueryRequest;
import com.bdis.modules.collection.dto.HerbBatchImageUpdateRequest;
import com.bdis.modules.collection.vo.HerbBatchImageBindResultVO;
import com.bdis.modules.collection.vo.HerbBatchImageStatisticsVO;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbImageBatchVO;

import java.util.List;

public interface HerbBatchImageService {

    HerbBatchImageVO bind(Long batchId, HerbBatchImageBindRequest request);

    HerbBatchImageBindResultVO batchBind(Long batchId, HerbBatchImageBatchBindRequest request);

    void unbind(Long batchId, Long imageId);

    List<HerbBatchImageVO> listByBatch(Long batchId, HerbBatchImageQueryRequest request);

    /** 按采集任务批量读取已绑定图片及已有识别结果。 */
    List<HerbBatchImageVO> listByTask(Long taskId);

    HerbImageBatchVO getBatchByImageId(Long imageId);

    HerbBatchImageVO setPrimary(Long batchId, Long imageId);

    HerbBatchImageVO update(Long batchId, Long imageId, HerbBatchImageUpdateRequest request);

    HerbBatchImageStatisticsVO refreshStatistics(Long batchId);
}
