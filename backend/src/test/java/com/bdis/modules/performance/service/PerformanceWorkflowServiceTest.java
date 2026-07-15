package com.bdis.modules.performance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.common.security.BusinessReferenceAccessService;
import com.bdis.modules.performance.dto.PerformanceRequest;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.entity.PerformanceStandardEntity;
import com.bdis.modules.performance.mapper.PerformanceAuditMapper;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.mapper.PerformanceParticipantMapper;
import com.bdis.modules.performance.mapper.PerformanceStandardMapper;
import com.bdis.modules.performance.service.impl.PerformanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PerformanceWorkflowServiceTest {

    @Mock private PerformanceMapper performanceMapper;
    @Mock private PerformanceStandardMapper standardMapper;
    @Mock private PerformanceParticipantMapper participantMapper;
    @Mock private PerformanceAuditMapper auditMapper;
    @Mock private PerformanceMaterialService materialService;
    @Mock private BusinessAccessService accessService;
    @Mock private BusinessReferenceAccessService referenceAccessService;
    @Mock private JdbcTemplate jdbcTemplate;

    @Test
    void creationUsesAuthenticatedUserAndAlwaysStartsAsDraft() {
        when(accessService.currentUserId()).thenReturn(5L);
        when(performanceMapper.selectById(8L)).thenAnswer(invocation -> inserted);
        PerformanceServiceImpl service =
                new PerformanceServiceImpl(
                        performanceMapper,
                        standardMapper,
                        participantMapper,
                        auditMapper,
                        materialService,
                        accessService,
                        referenceAccessService,
                        jdbcTemplate);
        PerformanceRequest request = new PerformanceRequest();
        request.setPerformanceTitle("测试业绩");

        doAnswer(
                        invocation -> {
                            inserted = invocation.getArgument(0);
                            inserted.setId(8L);
                            return 1;
                        })
                .when(performanceMapper)
                .insert(any(PerformanceEntity.class));
        PerformanceEntity result = service.createPerformance(request);

        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(result.getIdentifyStatus()).isEqualTo("draft");
        verify(participantMapper).insert(any(PerformanceParticipantEntity.class));
    }

    @Test
    void rejectedOrDraftSubmissionDoesNotCreateAuditRecordWhenConcurrentUpdateWins() {
        when(accessService.currentUserId()).thenReturn(5L);
        PerformanceEntity existing = new PerformanceEntity();
        existing.setId(8L);
        existing.setUserId(5L);
        existing.setIdentifyStatus("draft");
        existing.setStandardId(3L);
        when(performanceMapper.selectById(8L)).thenReturn(existing);
        when(performanceMapper.updateById(any(PerformanceEntity.class))).thenReturn(0);
        PerformanceStandardEntity standard = new PerformanceStandardEntity();
        standard.setLifecycleStatus("published");
        standard.setMaterialRequired(0);
        standard.setStandardNo("PSTD-1");
        standard.setStandardVersion(1);
        standard.setStandardName("测试标准");
        when(standardMapper.selectById(3L)).thenReturn(standard);
        when(materialService.listMaterials(8L)).thenReturn(java.util.List.of());
        PerformanceServiceImpl service = service();

        assertThatThrownBy(() -> service.submitPerformance(8L)).hasMessage("业绩状态已变更，请刷新后重试");
        verify(auditMapper, never())
                .insert(any(com.bdis.modules.performance.entity.PerformanceAuditEntity.class));
    }

    private PerformanceServiceImpl service() {
        return new PerformanceServiceImpl(
                performanceMapper,
                standardMapper,
                participantMapper,
                auditMapper,
                materialService,
                accessService,
                referenceAccessService,
                jdbcTemplate);
    }

    private PerformanceEntity inserted;
}
