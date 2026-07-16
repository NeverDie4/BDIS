package com.bdis.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.growth.entity.DigitalLifeNarrationEntity;
import com.bdis.modules.growth.mapper.DigitalLifeNarrationMapper;
import com.bdis.modules.growth.service.DigitalLifeNarrationGenerator.GeneratedNarration;
import com.bdis.modules.growth.service.impl.DigitalLifeNarrationServiceImpl;
import com.bdis.modules.growth.vo.DigitalLifeNarrationGenerationVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeMetricsVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DigitalLifeNarrationServiceImplTest {

    @Mock private HerbDigitalLifeArchiveService archiveService;
    @Mock private DigitalLifeNarrationMapper narrationMapper;
    @Mock private DigitalLifeNarrationGenerator narrationGenerator;

    private DigitalLifeNarrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new DigitalLifeNarrationServiceImpl(
                        archiveService, narrationMapper, narrationGenerator, new ObjectMapper());
        CurrentUser user =
                new CurrentUser(
                        7L,
                        "admin",
                        "管理员",
                        1L,
                        1L,
                        Set.of("ADMIN"),
                        Set.of(1L),
                        Set.of("growth:record:view"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unchangedSnapshotIsGeneratedOnceAndThenSkipped() {
        HerbDigitalLifeArchiveVO archive = archive(new BigDecimal("21.5"));
        List<DigitalLifeNarrationEntity> persisted = new java.util.ArrayList<>();
        when(archiveService.getByTaskId(9L)).thenReturn(archive);
        when(narrationMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of())
                .thenAnswer(invocation -> List.copyOf(persisted));
        when(narrationMapper.insert(any(DigitalLifeNarrationEntity.class)))
                .thenAnswer(
                        invocation -> {
                            persisted.add(invocation.getArgument(0));
                            return 1;
                        });
        when(narrationGenerator.generate(anyString(), anyString()))
                .thenReturn(new GeneratedNarration("规则摘要", "template", null));

        DigitalLifeNarrationGenerationVO first = service.generateForTask(9L);
        DigitalLifeNarrationGenerationVO second = service.generateForTask(9L);

        assertThat(first.generated()).isEqualTo(1);
        assertThat(second.skipped()).isEqualTo(1);
        ArgumentCaptor<DigitalLifeNarrationEntity> entityCaptor =
                ArgumentCaptor.forClass(DigitalLifeNarrationEntity.class);
        verify(narrationMapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getInputSnapshot()).hasSize(64);
        assertThat(entityCaptor.getValue().getNarrationSource()).isEqualTo("template");
        assertThat(entityCaptor.getValue().getGeneratedBy()).isEqualTo(7L);
        verify(narrationGenerator, times(1)).generate(anyString(), anyString());
        verify(narrationMapper, times(2)).selectList(any(Wrapper.class));
    }

    @Test
    void changedMetricInvalidatesSnapshotAndUpdatesCache() {
        HerbDigitalLifeArchiveVO archive = archive(new BigDecimal("21.5"));
        DigitalLifeNarrationEntity existing = new DigitalLifeNarrationEntity();
        existing.setId(3L);
        existing.setGrowthRecordId(11L);
        existing.setInputSnapshot("outdated");
        when(archiveService.getByTaskId(9L)).thenReturn(archive);
        when(narrationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));
        when(narrationGenerator.generate(anyString(), anyString()))
                .thenReturn(new GeneratedNarration("更新摘要", "ai", "model-a"));

        DigitalLifeNarrationGenerationVO result = service.generateForTask(9L);

        assertThat(result.generated()).isEqualTo(1);
        verify(narrationMapper).updateById(existing);
        assertThat(existing.getInputSnapshot()).hasSize(64).isNotEqualTo("outdated");
        assertThat(existing.getNarrationSource()).isEqualTo("ai");
        assertThat(existing.getModelName()).isEqualTo("model-a");
    }

    @Test
    void archiveWithoutGrowthRecordsDoesNotQueryOrWriteCache() {
        HerbDigitalLifeArchiveVO archive = new HerbDigitalLifeArchiveVO();
        archive.setTaskId(9L);
        archive.setStages(List.of(new HerbDigitalLifeStageVO()));
        when(archiveService.getByTaskId(9L)).thenReturn(archive);

        DigitalLifeNarrationGenerationVO result = service.generateForTask(9L);

        assertThat(result).isEqualTo(new DigitalLifeNarrationGenerationVO(0, 0, 0));
        verify(narrationMapper, never()).selectList(any(Wrapper.class));
        verify(narrationMapper, never()).insert(any(DigitalLifeNarrationEntity.class));
        verify(narrationGenerator, never()).generate(anyString(), anyString());
    }

    @Test
    void collectorCannotGenerateManagementNarrations() {
        CurrentUser collector =
                new CurrentUser(
                        8L,
                        "collector",
                        "采集员",
                        1L,
                        1L,
                        Set.of("COLLECTOR"),
                        Set.of(2L),
                        Set.of("growth:record:view"));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(collector, null, List.of()));

        assertThatThrownBy(() -> service.generateForTask(9L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("管理端角色");
        verify(archiveService, never()).getByTaskId(9L);
    }

    private HerbDigitalLifeArchiveVO archive(BigDecimal plantHeight) {
        HerbDigitalLifeMetricsVO metrics = new HerbDigitalLifeMetricsVO();
        metrics.setPlantHeight(plantHeight);
        metrics.setTemperature(new BigDecimal("24.0"));
        HerbDigitalLifeStageVO stage = new HerbDigitalLifeStageVO();
        stage.setSequence(1);
        stage.setBatchId(2L);
        stage.setGrowthRecordId(11L);
        stage.setCollectedAt(LocalDateTime.of(2026, 7, 13, 10, 0));
        stage.setGrowthStage("旺盛生长期");
        stage.setAuditStatus("approved");
        stage.setMetrics(metrics);
        HerbDigitalLifeArchiveVO archive = new HerbDigitalLifeArchiveVO();
        archive.setTaskId(9L);
        archive.setStages(List.of(stage));
        return archive;
    }
}
