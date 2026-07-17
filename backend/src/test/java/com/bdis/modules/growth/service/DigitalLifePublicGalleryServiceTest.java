package com.bdis.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.growth.service.impl.DigitalLifePublicGalleryService;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicSummaryVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class DigitalLifePublicGalleryServiceTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void listsPublicArchivesWithOneAggregatedQueryAndSafeKeywordBinding() {
        HerbDigitalLifePublicSummaryVO summary = new HerbDigitalLifePublicSummaryVO();
        summary.setTaskId(12L);
        summary.setTraceCode("DL-PUBLIC-001");
        summary.setSpeciesName("黄连");
        summary.setStageCount(3);
        summary.setStartTime(LocalDateTime.of(2026, 4, 15, 9, 0));
        summary.setEndTime(LocalDateTime.of(2026, 7, 15, 15, 33));
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(summary));

        DigitalLifePublicGalleryService service =
                new DigitalLifePublicGalleryService(jdbcTemplate);
        List<HerbDigitalLifePublicSummaryVO> result = service.list("黄连");

        assertThat(result).containsExactly(summary);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor =
                ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate, times(1))
                .query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains("t.public_visible = 1")
                .contains("gr.review_status = 'approved'")
                .contains("gr.public_visible = 1")
                .contains("COUNT(gr.id) AS stage_count")
                .contains(":keyword");
        assertThat(sqlCaptor.getValue()).doesNotContain("黄连");
        assertThat(paramsCaptor.getValue().getValue("keyword")).isEqualTo("%黄连%");
    }
}
