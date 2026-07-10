package com.bdis.modules.collection.mapper;

import com.bdis.modules.collection.dto.HerbBatchImageQueryRequest;
import com.bdis.modules.collection.entity.HerbBatchImageEntity;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbImageBatchVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbBatchImageMapper {

    int insert(HerbBatchImageEntity batchImage);

    int batchInsert(@Param("list") List<HerbBatchImageEntity> batchImages);

    int updateById(HerbBatchImageEntity batchImage);

    int updateIdentificationResultById(HerbBatchImageEntity batchImage);

    HerbBatchImageEntity selectById(@Param("id") Long id);

    List<HerbBatchImageEntity> selectByBatchId(@Param("batchId") Long batchId);

    HerbBatchImageEntity selectByImageId(@Param("imageId") Long imageId);

    HerbBatchImageEntity selectByBatchIdAndImageId(
            @Param("batchId") Long batchId, @Param("imageId") Long imageId);

    HerbBatchImageVO selectDetailByBatchIdAndImageId(
            @Param("batchId") Long batchId, @Param("imageId") Long imageId);

    HerbImageBatchVO selectBatchInfoByImageId(@Param("imageId") Long imageId);

    List<HerbBatchImageVO> selectBatchImagesWithIdentification(
            @Param("batchId") Long batchId, @Param("query") HerbBatchImageQueryRequest query);

    int logicDeleteById(@Param("id") Long id);

    int logicDeleteByBatchIdAndImageId(@Param("batchId") Long batchId, @Param("imageId") Long imageId);

    int clearPrimaryByBatchId(@Param("batchId") Long batchId);

    int updatePrimaryByBatchIdAndImageId(
            @Param("batchId") Long batchId, @Param("imageId") Long imageId);

    Long countByBatchId(@Param("batchId") Long batchId);

    Long countBoundByBatchId(@Param("batchId") Long batchId);
}
