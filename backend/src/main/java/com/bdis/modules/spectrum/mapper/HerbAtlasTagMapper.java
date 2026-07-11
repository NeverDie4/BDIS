package com.bdis.modules.spectrum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.spectrum.entity.SpectrumTagEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbAtlasTagMapper extends BaseMapper<SpectrumTagEntity> {

    int insertTags(@Param("tags") List<SpectrumTagEntity> tags);

    int logicalDeleteByAtlasId(SpectrumTagEntity tag);

    List<SpectrumTagEntity> selectByAtlasId(@Param("atlasId") Long atlasId);
}
