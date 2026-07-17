package com.bdis.modules.growth.service.impl;

import com.bdis.modules.growth.vo.HerbDigitalLifePublicSummaryVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DigitalLifePublicGalleryService {

    private static final String BASE_SQL =
            """
            SELECT
                t.id AS task_id,
                t.trace_code,
                t.task_code,
                t.task_name,
                t.species_name,
                t.base_name,
                t.description,
                COUNT(gr.id) AS stage_count,
                MIN(gr.collected_at) AS start_time,
                MAX(gr.collected_at) AS end_time
            FROM herb_collection_task t
            INNER JOIN herb_growth_record gr
                ON gr.task_id = t.id
                AND gr.is_deleted = 0
                AND gr.status = 1
                AND gr.review_status = 'approved'
                AND gr.public_visible = 1
            WHERE t.is_deleted = 0
                AND t.status = 1
                AND t.public_visible = 1
                AND t.trace_code IS NOT NULL
                AND t.trace_code <> ''
                AND t.task_status <> 'cancelled'
            """;

    private static final String KEYWORD_SQL =
            """
                AND (
                    t.task_name LIKE :keyword
                    OR t.species_name LIKE :keyword
                    OR t.base_name LIKE :keyword
                    OR t.task_code LIKE :keyword
                )
            """;

    private static final String GROUP_AND_ORDER_SQL =
            """
            GROUP BY
                t.id,
                t.trace_code,
                t.task_code,
                t.task_name,
                t.species_name,
                t.base_name,
                t.description
            ORDER BY MAX(gr.collected_at) DESC, t.id DESC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DigitalLifePublicGalleryService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HerbDigitalLifePublicSummaryVO> list(String keyword) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (hasKeyword) {
            parameters.addValue("keyword", "%" + keyword.trim() + "%");
        }
        String sql = BASE_SQL + (hasKeyword ? KEYWORD_SQL : "") + GROUP_AND_ORDER_SQL;
        return jdbcTemplate.query(sql, parameters, new SummaryRowMapper());
    }

    private static final class SummaryRowMapper
            implements RowMapper<HerbDigitalLifePublicSummaryVO> {

        @Override
        public HerbDigitalLifePublicSummaryVO mapRow(ResultSet resultSet, int rowNum)
                throws SQLException {
            HerbDigitalLifePublicSummaryVO summary = new HerbDigitalLifePublicSummaryVO();
            summary.setTaskId(resultSet.getLong("task_id"));
            summary.setTraceCode(resultSet.getString("trace_code"));
            summary.setTaskCode(resultSet.getString("task_code"));
            summary.setTaskName(resultSet.getString("task_name"));
            summary.setSpeciesName(resultSet.getString("species_name"));
            summary.setBaseName(resultSet.getString("base_name"));
            summary.setDescription(resultSet.getString("description"));
            summary.setStageCount(resultSet.getInt("stage_count"));
            summary.setStartTime(
                    resultSet.getTimestamp("start_time") == null
                            ? null
                            : resultSet.getTimestamp("start_time").toLocalDateTime());
            summary.setEndTime(
                    resultSet.getTimestamp("end_time") == null
                            ? null
                            : resultSet.getTimestamp("end_time").toLocalDateTime());
            return summary;
        }
    }
}
