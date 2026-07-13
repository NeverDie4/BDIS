package com.bdis.modules.herb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.herb.dto.HerbImageQueryRequest;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.vo.HerbImageVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbImageMapper extends BaseMapper<HerbImageEntity> {

    int insertImage(HerbImageEntity image);

    int updateImage(HerbImageEntity image);

    int updateProcessStatus(HerbImageEntity image);

    int logicalDeleteById(HerbImageEntity image);

    HerbImageEntity selectActiveById(@Param("id") Long id);

    HerbImageVO selectDetailById(@Param("id") Long id);

    List<HerbImageVO> selectByGrowthRecordId(@Param("growthRecordId") Long growthRecordId);

    List<HerbImageVO> selectByBatchId(@Param("batchId") Long batchId);

    Long countPage(
            @Param("query") HerbImageQueryRequest query,
            @Param("scope") CollectionAccessScope scope);

    List<HerbImageVO> selectPage(
            @Param("query") HerbImageQueryRequest query,
            @Param("scope") CollectionAccessScope scope,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);

    Long countByCollectorId(@Param("collectorId") Long collectorId);

    List<HerbImageVO> selectByCollectorId(
            @Param("collectorId") Long collectorId,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);

    List<HerbImageEntity> selectActiveForFeatureExtraction(@Param("speciesId") Long speciesId);
}
