package com.bdis.modules.collection.mapper;

import com.bdis.modules.collection.dto.HerbBatchQueryRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.vo.HerbBatchListVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbBatchMapper {

    int insert(HerbBatchEntity batch);

    int updateById(HerbBatchEntity batch);

    HerbBatchEntity selectById(@Param("id") Long id);

    HerbBatchVO selectDetailById(@Param("id") Long id);

    HerbBatchEntity selectByBatchCode(@Param("batchCode") String batchCode);

    Long countPage(
            @Param("query") HerbBatchQueryRequest query,
            @Param("scope") CollectionAccessScope scope);

    List<HerbBatchVO> selectPage(
            @Param("query") HerbBatchQueryRequest query,
            @Param("scope") CollectionAccessScope scope,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);

    List<HerbBatchListVO> selectList(
            @Param("query") HerbBatchQueryRequest query,
            @Param("scope") CollectionAccessScope scope);

    int logicDeleteById(@Param("id") Long id);

    int updateStatisticsById(HerbBatchEntity batch);

    int updateStatusById(HerbBatchEntity batch);

    int updateRemarkById(HerbBatchEntity batch);

    HerbBatchEntity selectBatchStatisticsById(@Param("id") Long id);

    Long countBoundImagesByBatchId(@Param("batchId") Long batchId);

    Long countUnfinishedByTaskId(@Param("taskId") Long taskId);

    int updateMobileSubmitFields(HerbBatchEntity batch);
}
