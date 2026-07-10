package com.bdis.modules.herb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.herb.entity.HerbEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HerbMapper extends BaseMapper<HerbEntity> {

    @Select(
            """
            SELECT *
            FROM herb_species
            WHERE is_deleted = 0
              AND herb_name = #{herbName}
            LIMIT 1
            """)
    HerbEntity selectByHerbName(@Param("herbName") String herbName);
}
