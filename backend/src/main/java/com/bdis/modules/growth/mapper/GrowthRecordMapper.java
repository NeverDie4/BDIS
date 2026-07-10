package com.bdis.modules.growth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GrowthRecordMapper extends BaseMapper<GrowthRecordEntity> {

    @Select(
            """
            SELECT
                id,
                species_id AS speciesId,
                distribution_id AS distributionId,
                collector_name_snapshot AS collectorName,
                longitude,
                latitude,
                growth_stage AS growthStage,
                soil_type AS soilType,
                soil_ph AS soilPh,
                temperature,
                humidity,
                weather,
                sample_weight AS sampleWeight,
                data_source AS dataSource,
                review_status AS reviewStatus,
                collected_at AS collectedAt,
                remark
            FROM herb_growth_record
            WHERE is_deleted = 0
              AND distribution_id = #{pointId}
            ORDER BY collected_at DESC, id DESC
            """)
    List<GrowthRecordVO> selectByPointId(@Param("pointId") Long pointId);
}
