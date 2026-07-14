package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bdis.audit.service.AuditLogService;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.service.impl.ResearchProjectServiceImpl;
import com.bdis.modules.research.vo.ResearchAchievementListVO;
import com.bdis.modules.research.vo.ResearchAchievementSummaryVO;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResearchProjectAchievementAggregationTest {
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private UserMapper userMapper;
    @Mock private HerbSpeciesMapper herbSpeciesMapper;
    @Mock private ProjectMemberService memberService;
    @Mock private ProjectMaterialService materialService;
    @Mock private ResearchAchievementService achievementService;
    @Mock private AuditLogService auditLogService;

    private ResearchProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ResearchProjectServiceImpl(
                        projectMapper,
                        memberMapper,
                        userMapper,
                        herbSpeciesMapper,
                        memberService,
                        materialService,
                        achievementService,
                        auditLogService);
    }

    @Test
    void projectDetailAggregatesAchievementsAndSummary() {
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(10L);
        project.setProjectNo("P-001");
        project.setProjectName("Research");
        project.setProjectStatus("ongoing");
        project.setStatus(1);
        project.setIsDeleted(0);
        when(projectMapper.selectById(10L)).thenReturn(project);
        when(memberService.list(10L, null)).thenReturn(List.of());
        when(materialService.list(10L, null)).thenReturn(List.of());
        ResearchAchievementListVO achievement = new ResearchAchievementListVO();
        achievement.setAchievementNo("A-001");
        when(achievementService.listByProjectId(10L)).thenReturn(List.of(achievement));
        ResearchAchievementSummaryVO summary = new ResearchAchievementSummaryVO();
        summary.setTotal(1);
        summary.setDraftCount(1);
        when(achievementService.summarizeByProjectId(10L)).thenReturn(summary);

        var result = service.getDetail(10L);

        assertThat(result.getAchievements()).hasSize(1);
        assertThat(result.getAchievementSummary().getTotal()).isEqualTo(1);
    }
}
