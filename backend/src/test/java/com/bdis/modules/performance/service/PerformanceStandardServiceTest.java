package com.bdis.modules.performance.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceStandardRequest;
import com.bdis.modules.performance.entity.PerformanceStandardEntity;
import com.bdis.modules.performance.mapper.PerformanceStandardMapper;
import com.bdis.modules.performance.service.impl.PerformanceStandardServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class PerformanceStandardServiceTest {

    @Mock private PerformanceStandardMapper standardMapper;
    @Mock private BusinessAccessService accessService;

    @Test
    void concurrentVersionConflictReturnsResourceConflictMessage() {
        PerformanceStandardEntity source = new PerformanceStandardEntity();
        source.setId(1L);
        source.setStandardNo("PSTD-1");
        source.setStandardVersion(1);
        source.setLifecycleStatus("published");
        when(standardMapper.selectById(1L)).thenReturn(source);
        when(standardMapper.selectList(any())).thenReturn(List.of(source));
        when(standardMapper.insert(any(PerformanceStandardEntity.class)))
                .thenThrow(new DuplicateKeyException("uk_perf_standard_no_version"));
        PerformanceStandardRequest request = new PerformanceStandardRequest();
        request.setStandardName("新版本");
        request.setPerformanceType("RESEARCH");

        assertThatThrownBy(
                        () ->
                                new PerformanceStandardServiceImpl(standardMapper, accessService)
                                        .createVersion(1L, request))
                .hasMessage("标准版本已被其他请求创建，请刷新后重试");
    }

    @Test
    void draftStandardMustBeEditedInsteadOfCreatingAnotherDraftVersion() {
        PerformanceStandardEntity source = new PerformanceStandardEntity();
        source.setId(1L);
        source.setStandardNo("PSTD-1");
        source.setStandardVersion(1);
        source.setLifecycleStatus("draft");
        when(standardMapper.selectById(1L)).thenReturn(source);
        PerformanceStandardRequest request = new PerformanceStandardRequest();
        request.setStandardName("新版本");
        request.setPerformanceType("RESEARCH");

        assertThatThrownBy(
                        () ->
                                new PerformanceStandardServiceImpl(standardMapper, accessService)
                                        .createVersion(1L, request))
                .hasMessage("草稿标准应直接编辑，发布或停用后才能创建新版本");
    }
}
