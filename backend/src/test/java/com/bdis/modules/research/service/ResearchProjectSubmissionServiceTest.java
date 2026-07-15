package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.mapper.ResearchProjectSubmissionMapper;
import com.bdis.modules.research.request.ResearchProjectSubmissionCreateRequest;
import com.bdis.modules.research.service.impl.ResearchProjectSubmissionServiceImpl;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ResearchProjectSubmissionServiceTest {
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private ResearchProjectSubmissionMapper submissionMapper;
    private ResearchProjectSubmissionService service;

    @BeforeEach void setUp() { service = new ResearchProjectSubmissionServiceImpl(projectMapper, memberMapper, submissionMapper); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void acceptedMemberCanSubmitStageReport() {
        setUser(8L, "STUDENT");
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(11L); project.setLeaderId(3L); project.setProjectStatus("ongoing"); project.setStatus(1); project.setIsDeleted(0);
        when(projectMapper.selectById(11L)).thenReturn(project);
        when(projectMapper.existsActiveMember(11L, 8L)).thenReturn(true);
        when(submissionMapper.insert(any(ResearchProjectSubmissionEntity.class))).thenAnswer(invocation -> {
            ((ResearchProjectSubmissionEntity) invocation.getArgument(0)).setId(41L); return 1;
        });
        ResearchProjectSubmissionCreateRequest request = new ResearchProjectSubmissionCreateRequest();
        request.setSubmissionType("stage_report"); request.setSubmissionTitle("阶段报告"); request.setContent("result");
        assertThat(service.submit(11L, request)).isEqualTo(41L);
    }

    private void setUser(Long id, String role) {
        CurrentUser user = new CurrentUser(id, "user-" + id, "User", null, null, Set.of(role), Set.of(), Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }
}
