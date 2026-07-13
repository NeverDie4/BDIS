package com.bdis.modules.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.modules.course.service.CourseResourceService;
import com.bdis.modules.course.vo.CourseResourceVO;
import com.bdis.modules.permission.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CourseResourceControllerTest {

    private MockMvc mockMvc;

    @Mock private CourseResourceService resourceService;

    @Mock private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new CourseResourceController(resourceService, authorizationService))
                        .build();
    }

    @Test
    void bindResourceReturnsUnifiedResultAndChecksPermission() throws Exception {
        CourseResourceVO vo = new CourseResourceVO();
        vo.setId(41L);
        vo.setCourseId(11L);
        vo.setFileId(31L);
        when(resourceService.bind(any(), any())).thenReturn(vo);

        mockMvc.perform(
                        post("/courses/11/resources")
                                .contentType("application/json")
                                .content(
                                        "{\"fileId\":31,\"resourceName\":\"Experiment handout\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fileId").value(31));

        verify(authorizationService).requirePermission("edu:course-resource:add");
    }
}
