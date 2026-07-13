package com.bdis.modules.map.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.vo.MapPointVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MapPointMapper extends BaseMapper<MapPointEntity> {

    @Select(
            """
            <script>
            SELECT
                d.id,
                d.species_id AS speciesId,
                h.herb_name AS herbName,
                h.alias_name AS aliasName,
                h.latin_name AS latinName,
                h.medicinal_part AS medicinalPart,
                h.efficacy AS efficacy,
                h.growth_environment AS growthEnvironment,
                h.origin_area AS originArea,
                h.growth_cycle AS growthCycle,
                h.description AS herbDescription,
                d.base_id AS baseId,
                b.base_name AS baseName,
                d.region_id AS regionId,
                d.location_name AS locationName,
                d.longitude,
                d.latitude,
                d.province,
                d.city,
                d.district,
                d.address,
                d.altitude,
                d.distribution_type AS distributionType,
                d.distribution_level AS distributionLevel,
                d.distribution_desc AS distributionDesc,
                d.cover_image_url AS coverImageUrl,
                d.last_collected_at AS lastCollectedAt,
                d.source_type AS sourceType,
                d.data_source AS dataSource,
                d.status,
                d.remark
            FROM herb_distribution d
            LEFT JOIN herb_species h ON h.id = d.species_id AND h.is_deleted = 0
            LEFT JOIN herb_base b ON b.id = d.base_id AND b.is_deleted = 0
            WHERE d.is_deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (
                h.herb_name LIKE CONCAT('%', #{keyword}, '%')
                OR h.alias_name LIKE CONCAT('%', #{keyword}, '%')
                OR d.location_name LIKE CONCAT('%', #{keyword}, '%')
                OR d.address LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="district != null and district != ''">
              AND d.district = #{district}
            </if>
            <if test="speciesId != null">
              AND d.species_id = #{speciesId}
            </if>
            <if test="baseId != null">
              AND d.base_id = #{baseId}
            </if>
            <if test="includeDisabled == null or includeDisabled == false">
              AND d.status = 1
            </if>
            ORDER BY d.updated_at DESC, d.id DESC
            </script>
            """)
    List<MapPointVO> selectMapPoints(
            @Param("keyword") String keyword,
            @Param("district") String district,
            @Param("speciesId") Long speciesId,
            @Param("baseId") Long baseId,
            @Param("includeDisabled") Boolean includeDisabled);
}
