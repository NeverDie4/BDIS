package com.bdis.modules.herb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.herb.dto.HerbSpeciesQueryRequest;
import com.bdis.modules.herb.entity.HerbEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbSpeciesMapper extends BaseMapper<HerbEntity> {

    int insertSpecies(HerbEntity herbSpecies);

    int updateSpecies(HerbEntity herbSpecies);

    int logicalDeleteById(HerbEntity herbSpecies);

    HerbEntity selectActiveById(@Param("id") Long id);

    HerbEntity selectByHerbCode(@Param("herbCode") String herbCode);

    HerbEntity selectByNameOrAlias(@Param("keyword") String keyword);

    Long countPage(@Param("query") HerbSpeciesQueryRequest query);

    List<HerbEntity> selectPage(
            @Param("query") HerbSpeciesQueryRequest query,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);

    List<HerbEntity> selectEnabledList();
}
