package com.bdis.modules.performance.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerformanceFileBusinessAccessPolicyTest {

    @Mock private PerformanceMapper performanceMapper;
    @Mock private BusinessAccessService accessService;

    @Test
    void submittedPerformanceFreezesFileAttachmentsAcrossGenericFileEndpoints() {
        PerformanceEntity performance = performance("submitted");
        when(performanceMapper.selectById(8L)).thenReturn(performance);

        PerformanceFileBusinessAccessPolicy policy = policy();

        assertThat(policy.canAttach(8L)).isFalse();
        assertThat(policy.canDetach(8L)).isFalse();
        verify(accessService, never())
                .requireResourceAccess(
                        "perf_record", 8L, "performance:record:update", performance.getUserId());
    }

    @Test
    void draftPerformanceAllowsAuthorizedFileAttachmentChanges() {
        PerformanceEntity performance = performance("draft");
        when(performanceMapper.selectById(8L)).thenReturn(performance);

        PerformanceFileBusinessAccessPolicy policy = policy();

        assertThat(policy.canAttach(8L)).isTrue();
        assertThat(policy.canDetach(8L)).isTrue();
        verify(accessService, times(2))
                .requireResourceAccess(
                        "perf_record", 8L, "performance:record:update", performance.getUserId());
    }

    @Test
    void performanceMaterialsCannotBePublishedAsAnonymousFiles() {
        assertThat(policy().canPublish(8L)).isFalse();
        verify(performanceMapper, never()).selectById(8L);
    }

    private PerformanceFileBusinessAccessPolicy policy() {
        return new PerformanceFileBusinessAccessPolicy(performanceMapper, accessService);
    }

    private PerformanceEntity performance(String status) {
        PerformanceEntity performance = new PerformanceEntity();
        performance.setId(8L);
        performance.setUserId(5L);
        performance.setIdentifyStatus(status);
        return performance;
    }
}
