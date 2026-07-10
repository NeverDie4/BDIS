package com.bdis.modules.spectrum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.spectrum.entity.HerbImageFeatureEntity;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import org.apache.ibatis.annotations.Param;

public interface HerbImageFeatureMapper extends BaseMapper<HerbImageFeatureEntity> {

    int insertFeature(HerbImageFeatureEntity feature);

    int deleteActiveByImageIdAndModel(
            @Param("imageId") Long imageId,
            @Param("featureModel") String featureModel,
            @Param("featureVersion") String featureVersion);

    int existsSuccessByImageIdAndModel(
            @Param("imageId") Long imageId,
            @Param("featureModel") String featureModel,
            @Param("featureVersion") String featureVersion);

    FeatureExtractResultVO selectLatestSuccessByImageId(@Param("imageId") Long imageId);
}
