package com.bdis.modules.spectrum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.spectrum.dto.HerbImageMatchQueryRequest;
import com.bdis.modules.spectrum.entity.SpectrumComparisonEntity;
import com.bdis.modules.spectrum.vo.HerbAtlasFeatureCandidateVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchPageVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbImageMatchMapper extends BaseMapper<SpectrumComparisonEntity> {

    int insertMatch(SpectrumComparisonEntity match);

    int batchInsertMatches(@Param("matches") List<SpectrumComparisonEntity> matches);

    int countActiveByImageId(@Param("imageId") Long imageId);

    List<HerbAtlasFeatureCandidateVO> selectAtlasFeatureCandidates(
            @Param("speciesId") Long speciesId);

    List<HerbImageMatchPageVO> selectLatestByImageId(@Param("imageId") Long imageId);

    List<HerbImageMatchPageVO> selectByImageId(@Param("imageId") Long imageId);

    Long countPage(
            @Param("query") HerbImageMatchQueryRequest query,
            @Param("scope") CollectionAccessScope scope);

    List<HerbImageMatchPageVO> selectPage(
            @Param("query") HerbImageMatchQueryRequest query,
            @Param("scope") CollectionAccessScope scope,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);
}
