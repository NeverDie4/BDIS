package com.bdis.modules.spectrum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.spectrum.dto.HerbIdentificationQueryRequest;
import com.bdis.modules.spectrum.entity.HerbIdentificationResultEntity;
import com.bdis.modules.spectrum.vo.HerbIdentificationPageVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbIdentificationResultMapper
        extends BaseMapper<HerbIdentificationResultEntity> {

    int insertResult(HerbIdentificationResultEntity result);

    int updateReviewResult(HerbIdentificationResultEntity result);

    HerbIdentificationResultEntity selectActiveById(@Param("id") Long id);

    HerbIdentificationPageVO selectLatestByImageId(@Param("imageId") Long imageId);

    List<HerbIdentificationPageVO> selectByImageId(@Param("imageId") Long imageId);

    Long countPage(@Param("query") HerbIdentificationQueryRequest query);

    List<HerbIdentificationPageVO> selectPage(
            @Param("query") HerbIdentificationQueryRequest query,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);
}
