package com.bdis.modules.spectrum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.spectrum.entity.HerbAtlasFeatureEntity;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import org.apache.ibatis.annotations.Param;

public interface HerbAtlasFeatureMapper extends BaseMapper<HerbAtlasFeatureEntity> {

    int insertFeature(HerbAtlasFeatureEntity feature);

    int deleteActiveByAtlasIdAndModel(
            @Param("atlasId") Long atlasId,
            @Param("featureModel") String featureModel,
            @Param("featureVersion") String featureVersion);

    int existsSuccessByAtlasIdAndModel(
            @Param("atlasId") Long atlasId,
            @Param("featureModel") String featureModel,
            @Param("featureVersion") String featureVersion);

    FeatureExtractResultVO selectLatestSuccessByAtlasId(@Param("atlasId") Long atlasId);
}
