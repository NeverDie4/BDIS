package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.research.entity.ResearchAchievementEntity;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ResearchAchievementMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.request.ResearchAchievementCreateRequest;
import com.bdis.modules.research.request.ResearchAchievementUpdateRequest;
import com.bdis.modules.research.service.impl.ResearchAchievementServiceImpl;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.modules.research.vo.ResearchAchievementDetailVO;
import com.bdis.modules.research.vo.ResearchAchievementSummaryVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResearchAchievementServiceTest {

    @Mock private ResearchAchievementMapper achievementMapper;
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private FileResourceMapper fileResourceMapper;
    @Mock private FileAccessGuard fileAccessGuard;
    @Mock private AuditLogService auditLogService;

    private ResearchAchievementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ResearchAchievementServiceImpl(
                achievementMapper, projectMapper, fileResourceMapper, fileAccessGuard,
                auditLogService);
    }

    @Test
    void createForcesDraftAndRecordsAudit() {
        when(achievementMapper.selectByAchievementNoIncludingDeleted("A-001")).thenReturn(null);
        when(projectMapper.selectById(10L)).thenReturn(project(10L, "ongoing"));
        when(achievementMapper.insert(any(ResearchAchievementEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ResearchAchievementEntity.class).setId(20L);
            return 1;
        });

        Long id = service.create(createRequest("A-001", 10L));

        assertThat(id).isEqualTo(20L);
        verify(achievementMapper).insert(any(ResearchAchievementEntity.class));
        verify(auditLogService).record(any());
    }

    @Test
    void deletedAchievementNumberCannotBeReused() {
        ResearchAchievementEntity deleted = new ResearchAchievementEntity();
        deleted.setId(99L);
        deleted.setIsDeleted(1);
        when(achievementMapper.selectByAchievementNoIncludingDeleted("A-001")).thenReturn(deleted);

        assertThatThrownBy(() -> service.create(createRequest("A-001", 10L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(achievementMapper, never()).insert(any(ResearchAchievementEntity.class));
    }

    @Test
    void invalidTypeOrStageIsRejected() {
        ResearchAchievementCreateRequest request = createRequest("A-001", 10L);
        request.setAchievementType("book");
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessException.class);

        request.setAchievementType("paper");
        request.setAchievementStage("unknown");
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void completedProjectIsReadOnlyForAchievementWrites() {
        when(achievementMapper.selectByAchievementNoIncludingDeleted("A-001")).thenReturn(null);
        when(projectMapper.selectById(10L)).thenReturn(project(10L, "completed"));

        assertThatThrownBy(() -> service.create(createRequest("A-001", 10L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void inactiveFileIsRejected() {
        when(achievementMapper.selectByAchievementNoIncludingDeleted("A-001")).thenReturn(null);
        when(projectMapper.selectById(10L)).thenReturn(project(10L, "ongoing"));
        FileResourceEntity file = new FileResourceEntity();
        file.setId(30L);
        file.setStatus(0);
        file.setIsDeleted(0);
        when(fileResourceMapper.selectById(30L)).thenReturn(file);
        ResearchAchievementCreateRequest request = createRequest("A-001", 10L);
        request.setFileId(30L);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(Exception.class);
    }

    @Test
    void updateAllowsDraftToSubmittedButRejectsInvalidTransitionAndConfirmedMutation() {
        ResearchAchievementEntity existing = achievement(20L, "draft");
        when(achievementMapper.selectById(20L)).thenReturn(existing);
        when(projectMapper.selectById(10L)).thenReturn(project(10L, "ongoing"));
        when(achievementMapper.updateById(existing)).thenReturn(1);
        ResearchAchievementUpdateRequest request = updateRequest("submitted", 0);

        service.update(20L, request);

        assertThat(existing.getAchievementStatus()).isEqualTo("submitted");
        verify(auditLogService).record(any());

        existing.setAchievementStatus("confirmed");
        assertThatThrownBy(() -> service.update(20L, updateRequest("confirmed", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("confirmed");
    }

    @Test
    void detailAndSummaryAreAvailableForReadOnlyProject() {
        ResearchAchievementEntity entity = achievement(20L, "confirmed");
        when(achievementMapper.selectById(20L)).thenReturn(entity);
        when(projectMapper.selectById(10L)).thenReturn(project(10L, "completed"));
        ResearchAchievementDetailVO detail = service.getDetail(20L);

        assertThat(detail.getAchievementNo()).isEqualTo("A-001");
        assertThat(detail.getAchievementStatus()).isEqualTo("confirmed");

        when(achievementMapper.selectList(any())).thenReturn(List.of(
                achievement(20L, "draft"), achievement(21L, "submitted"), achievement(22L, "confirmed")));
        ResearchAchievementSummaryVO summary = service.summarizeByProjectId(10L);
        assertThat(summary.getTotal()).isEqualTo(3);
        assertThat(summary.getDraftCount()).isEqualTo(1);
        assertThat(summary.getSubmittedCount()).isEqualTo(1);
        assertThat(summary.getConfirmedCount()).isEqualTo(1);
    }

    private ResearchAchievementCreateRequest createRequest(String no, Long projectId) {
        ResearchAchievementCreateRequest request = new ResearchAchievementCreateRequest();
        request.setAchievementNo(no);
        request.setProjectId(projectId);
        request.setAchievementName("Paper");
        request.setAchievementType("paper");
        request.setAchievementStage("initial");
        return request;
    }

    private ResearchAchievementUpdateRequest updateRequest(String status, Integer version) {
        ResearchAchievementUpdateRequest request = new ResearchAchievementUpdateRequest();
        request.setAchievementName("Updated");
        request.setAchievementType("paper");
        request.setAchievementStatus(status);
        request.setVersion(version);
        return request;
    }

    private ResearchProjectEntity project(Long id, String status) {
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(id);
        project.setProjectStatus(status);
        project.setStatus(1);
        project.setIsDeleted(0);
        return project;
    }

    private ResearchAchievementEntity achievement(Long id, String status) {
        ResearchAchievementEntity achievement = new ResearchAchievementEntity();
        achievement.setId(id);
        achievement.setAchievementNo("A-001");
        achievement.setProjectId(10L);
        achievement.setAchievementName("Paper");
        achievement.setAchievementType("paper");
        achievement.setAchievementStage("initial");
        achievement.setAchievementStatus(status);
        achievement.setIsDeleted(0);
        achievement.setStatus(1);
        achievement.setVersion(0);
        return achievement;
    }
}
