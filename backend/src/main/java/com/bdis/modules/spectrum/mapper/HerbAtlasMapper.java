package com.bdis.modules.spectrum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.spectrum.dto.HerbAtlasQueryRequest;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.vo.HerbAtlasVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbAtlasMapper extends BaseMapper<SpectrumEntity> {

    int insertAtlas(SpectrumEntity atlas);

    int updateAtlas(SpectrumEntity atlas);

    int logicalDeleteById(SpectrumEntity atlas);

    SpectrumEntity selectActiveById(@Param("id") Long id);

    int existsBySpeciesIdAndImageName(
            @Param("speciesId") Long speciesId, @Param("imageName") String imageName);

    int countByAtlasCode(@Param("atlasCode") String atlasCode);

    Long countPage(@Param("query") HerbAtlasQueryRequest query);

    List<HerbAtlasVO> selectPage(
            @Param("query") HerbAtlasQueryRequest query,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);

    HerbAtlasVO selectDetailById(@Param("id") Long id);

    List<HerbAtlasVO> selectEnabledBySpeciesId(@Param("speciesId") Long speciesId);

    List<SpectrumEntity> selectFeatureCandidates();

    List<SpectrumEntity> selectEnabledForFeatureExtraction(@Param("speciesId") Long speciesId);

    List<SpectrumEntity> selectLegacyFileCandidates();

    int updateImageUrl(@Param("id") Long id, @Param("imageUrl") String imageUrl);
}
