package com.bdis.modules.growth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.vo.GrowthChartPointVO;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GrowthRecordMapper extends BaseMapper<GrowthRecordEntity> {

    @Select(
            """
            SELECT COUNT(*)
            FROM herb_growth_record
            WHERE task_id = #{taskId}
              AND status = 1
              AND is_deleted = 0
              AND review_status = 'approved'
              AND public_visible = 1
            """)
    long countPublicDigitalLifeStages(@Param("taskId") Long taskId);

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

    @Select(
            """
            SELECT gr.*, b.batch_name AS batchName, t.task_name AS taskName,
                   t.collect_place AS collectPlace,
                   gr.collector_name_snapshot AS collectorName
            FROM herb_growth_record gr
            LEFT JOIN herb_batch b ON b.id = gr.batch_id AND b.is_deleted = 0
            LEFT JOIN herb_collection_task t ON t.id = gr.task_id AND t.is_deleted = 0
            WHERE gr.id = #{id} AND gr.is_deleted = 0
            """)
    GrowthRecordVO selectJoinedById(@Param("id") Long id);

    @Select(
            """
            SELECT gr.*, b.batch_name AS batchName, t.task_name AS taskName,
                   t.collect_place AS collectPlace,
                   gr.collector_name_snapshot AS collectorName
            FROM herb_growth_record gr
            LEFT JOIN herb_batch b ON b.id = gr.batch_id AND b.is_deleted = 0
            LEFT JOIN herb_collection_task t ON t.id = gr.task_id AND t.is_deleted = 0
            WHERE gr.batch_id = #{batchId} AND gr.is_deleted = 0
            """)
    GrowthRecordVO selectJoinedByBatchId(@Param("batchId") Long batchId);

    @Select(
            """
            <script>
            SELECT gr.*, b.batch_name AS batchName, t.task_name AS taskName,
                   t.collect_place AS collectPlace,
                   gr.collector_name_snapshot AS collectorName
            FROM herb_growth_record gr
            LEFT JOIN herb_batch b ON b.id = gr.batch_id AND b.is_deleted = 0
            LEFT JOIN herb_collection_task t ON t.id = gr.task_id AND t.is_deleted = 0
            WHERE gr.batch_id IN
            <foreach collection="batchIds" item="batchId" open="(" separator="," close=")">
                #{batchId}
            </foreach>
              AND gr.is_deleted = 0
            ORDER BY gr.batch_id, gr.collected_at, gr.id
            </script>
            """)
    List<GrowthRecordVO> selectJoinedByBatchIds(@Param("batchIds") List<Long> batchIds);

    @Select(
            """
            <script>
            SELECT gr.id AS recordId, gr.task_id AS taskId, gr.batch_id AS batchId,
                   b.batch_name AS batchName,
                   COALESCE(gr.collected_at, b.collect_start_time) AS collectTime,
                   gr.plant_height AS plantHeight, gr.temperature,
                   gr.humidity, gr.soil_moisture AS soilMoisture,
                   gr.soil_ph AS soilPh, gr.light, gr.sample_weight AS sampleWeight,
                   gr.growth_stage AS growthStage,
                   gr.collector_name_snapshot AS collectorName,
                   gr.review_status AS auditStatus,
                   COALESCE(NULLIF(gr.growth_evaluation, ''), NULLIF(gr.remark, ''),
                            CONCAT('批次 ', b.batch_name, ' 生长采集记录')) AS summary
            FROM herb_growth_record gr
            JOIN herb_batch b ON b.id = gr.batch_id AND b.is_deleted = 0
            JOIN herb_collection_task t ON t.id = gr.task_id AND t.is_deleted = 0
            WHERE gr.task_id = #{taskId} AND gr.is_deleted = 0
              AND gr.id IN
              <foreach collection="recordIds" item="recordId" open="(" separator="," close=")">
                #{recordId}
              </foreach>
            ORDER BY COALESCE(gr.collected_at, b.collect_start_time), gr.id
            </script>
            """)
    List<GrowthChartPointVO> selectChartPoints(
            @Param("taskId") Long taskId, @Param("recordIds") List<Long> recordIds);
}
